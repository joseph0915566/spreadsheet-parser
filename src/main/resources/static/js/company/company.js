(() => {

    //DOM elements
    const btnUpload = document.getElementById("btnUpload");
    const inputFile = document.getElementById("payload");
    const inputLdap = document.getElementById("ldap");
    const progressbarTable = document.getElementById("progressbar-table");

    //constants representing event type returned to company.js
    const UPLOAD_PROGRESS = 'PROGRESS';
    const WORKER_RETURN = 'WORKER RETURN';
    const UPLOADID = 'SHARE UPLOAD ID';

    let uploadId = "";
    function handleWorkerResponse(e){
        switch (e.data.type){
            case UPLOAD_PROGRESS:
                getUploadProgress(e);
                break;
            case WORKER_RETURN:
                inputFile.value = '';
                progressbarTable.setHTML('');
                alert("File has been uploaded. You'll be notified via email once the result is ready for download.");
                enableDisableUpload();
                break;
            case UPLOADID:
                uploadId = e.data.id;
                break;
        }
    }

    function getUploadProgress(e){

        const partNumber = e.data.partNumber;
        let meter = document.getElementById("meter-" + partNumber);
        let text = document.getElementById("text-" + partNumber);
        let meterFullLength = document.getElementById("container-" + partNumber).clientWidth - 2 * meter.offsetLeft;
        let percentage = Math.round(e.data.loaded * 100 / e.data.total);
        meter.style.width = (meterFullLength * percentage / 100) + 'px';
        text.textContent = percentage + '%';
        if(e.data.loaded == e.data.total) meter.style.backgroundColor = 'lime';

    }

    function enableDisableUpload(){
        if(btnUpload.hasAttribute('disabled')) btnUpload.removeAttribute('disabled');
        else btnUpload.setAttribute('disabled', '');
    }

    function drawProgressBar(partCount){

        let innerHtml = '';
        for(let i = 1 ; i <= partCount ; i++){
            innerHtml +=
                '    <div class="progressbar-section">\n' +
                '        <div class = "progressbar-caption">\n' +
                '            <div id="part-' + i + '" class="progressbar-part text">Part ' + i + '</div><div id="retry-' + i + '" class="progressbar-retry text">Fails: 0</div>\n' +
                '        </div>\n' +
                '        <div id="container-' + i + '" class="progressbar-container">\n' +
                '            <div id="meter-' + i + '" class="progressbar-meter"></div>\n' +
                '            <div id="text-' + i + '" class="progressbar-text text">0%</div>\n' +
                '        </div>\n' +
                '    </div>';

        }

        progressbarTable.setHTML(innerHtml);

    }

    window.onload = function(event){

        btnUpload.addEventListener("click", async function (){

            //disable button
            btnUpload.setAttribute('disabled', '');

            const file = inputFile.files[0];
            if(!file) {
                alert('Please select a file to upload');
                btnUpload.removeAttribute('disabled');
                return;
            }
            const ldap = inputLdap.value.trim();
            if(!ldap.length){
                alert("Please fill LDAP field");
                return;
            }

            const chunkSize = 15 * 1024 * 1024;

            //draw the progress bars
            drawProgressBar(Math.ceil(file.size / chunkSize * 1.3));

            //initiate multipart upload process to S3
            let worker = new Worker('/js/company/multipart-upload-worker.js', {type: 'module'});
            worker.onmessage = e => {handleWorkerResponse(e)}
            worker.postMessage({
                file: file,
                chunkSize: chunkSize,
                ldap: ldap,
                host: window.location.protocol + '//' + window.location.hostname,
            });

        });

    };

})()
