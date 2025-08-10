import {RetryableLimiter} from "/js/retryable-limiter.js";
import {Log} from "/js/log.js"
import {HttpClient} from "/js/httprequest.js";
import {S3UploadTaskGenerator} from "/js/s3-upload-task-generator.js";

//constants representing event type returned to company.js
const UPLOAD_PROGRESS = 'PROGRESS';
const WORKER_RETURN = 'WORKER RETURN';
const UPLOADID = 'SHARE UPLOAD ID';

onmessage = async(e) => {

    const chunkSize = e.data.chunkSize;
    const ldap = e.data.ldap;
    const file = e.data.file;
    const threadCount = navigator.hardwareConcurrency < 5 ? 1 : navigator.hardwareConcurrency - 3;
    const retryCount = 3;
    const retryDelay = 20 * 1000;
    const requestTimeout = 15 * 60 * 1000;

    //initiate multipart request to S3
    const selfService = new HttpClient(e.data.host, 5000);
    let estimatedPartCount = Math.ceil(file.size / chunkSize * 1.3);
    const uploadDetails = (await selfService.doRequest({
        url: "/company/init-multipart-upload",
        method: "POST",
        payload: {
            partCount: estimatedPartCount,
            filename: ldap + '/' + file.name
        },
    }));

    const uploadId = uploadDetails.data.uploadId;
    const presignedUrls = uploadDetails.data.presignedUrls;

    postMessage({type: UPLOADID, id: uploadId});

    const taskGenerator = new S3UploadTaskGenerator(
                                                    presignedUrls,
                                                    file,
                                                    chunkSize,
                                                    requestTimeout,
                                                    (client, url, payload) => async () => {
                                                        const params = url.searchParams;
                                                        const partNumber = params.get('partNumber');
                                                        return {
                                                            response: await client.doRequest(
                                                                {
                                                                    url: url.pathname,
                                                                    params: params,
                                                                    method: 'PUT',
                                                                    partNumber: partNumber,
                                                                    payload: payload.records,
                                                                    uploadProgressListener: e => {
                                                                        postMessage({type: UPLOAD_PROGRESS, partNumber: partNumber, loaded: e.loaded, total: e.total});
                                                                    }
                                                                }
                                                            ),
                                                            partNumber: partNumber,
                                                            lastRow: payload.lastRow
                                                        };
                                                    }
    ).get();

    const limiter = new RetryableLimiter(taskGenerator, threadCount, retryCount, retryDelay);
    const filename = new URL(presignedUrls[1]).pathname.substring(1);
    const completeMultipartRequest = {
        uploadId: uploadId,
        filename: filename,
        parts: Array(estimatedPartCount)
    };
    let rowCount = 0;
    while(true){

        const result = (await limiter.result().next()).value;
        if(result === undefined) break;

        const [payload, task] = result;
        if(task === undefined){
            const partNumber = parseInt(payload.partNumber);
            completeMultipartRequest.parts[partNumber - 1] = {
                partNumber: partNumber,
                eTag: payload.response.headers.etag
            };
            if(payload.lastRow > rowCount) rowCount = payload.lastRow;
        }
        else {
            Log.warn('[FAILED] ' + JSON.stringify(task) + ' has failed ' + payload);
            if(!task.retriesLeft){
                Log.warn('ABORTING!!!');
                return;
            } else {
                task.retriesLeft--;
                limiter.retryTask(task);
            }
        }

    }

    //compacting array
    while(--estimatedPartCount){
        if(!(estimatedPartCount in completeMultipartRequest.parts)) completeMultipartRequest.parts.length--;
        else break;
    }

    postMessage({type: WORKER_RETURN});

    await selfService.doRequest({
        url: "/company/complete-multipart-upload",
        method: "POST",
        payload: completeMultipartRequest
    });

    selfService.doRequest({
        url: "/company/process-csv",
        method: "POST",
        payload: {key: filename, totalRows: rowCount, email: ldap + '@company.com'}
    });

}

onerror = err => {
    postMessage({type: WORKER_RETURN, error: err});
}