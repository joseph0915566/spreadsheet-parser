import {HttpClient} from "/js/httprequest.js";

export const S3UploadTaskGenerator = (function (){

    function S3UploadTaskGenerator(presignedUrls, file, chunkSize, requestTimeout, taskTemplate){
        this._presignedUrls = presignedUrls;
        this._file = file;
        this._chunkSize = chunkSize;
        this._requestTimeout = requestTimeout;
        this._taskTemplate = taskTemplate;
    }

    async function* generateTask(){

        const csvWorker = new Worker("/js/pausable-csv-worker.js", {type: 'module'});
        const getCsvBatch = () => new Promise(resolver => {

            const channel = new MessageChannel();
            channel.port2.onmessage = e => {resolver(e.data);};

            csvWorker.postMessage({type: 'GET'}, [channel.port1]);

        });

        //initiate csv parser
        csvWorker.postMessage({type:'INIT', file: this._file, chunkSize: this._chunkSize});

        const baseUrl = new URL(this._presignedUrls[1]);
        const client = new HttpClient(baseUrl.protocol + '//' + baseUrl.host, this._requestTimeout);
        let counter = 1;
        while(true){
            const payload = await getCsvBatch();
            if(payload.hasOwnProperty('records')) yield this._taskTemplate(client, new URL(this._presignedUrls[counter++]), payload);
            if(payload.hasOwnProperty('isComplete')) break;
        }

        csvWorker.terminate();

    }

    S3UploadTaskGenerator.prototype.get = function(){
        return generateTask.call(this);
    }

    return S3UploadTaskGenerator;

})();
