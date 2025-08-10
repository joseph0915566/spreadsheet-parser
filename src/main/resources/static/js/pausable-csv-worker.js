import {Papa} from "/js/third-party/papaparse.min.js"

const PausableCsvWorker = (function(){

    function PausableCsvParser(file, chunkSize){
        this._file = file;
        this._chunkSize = chunkSize;
        this._parser = undefined;
        createPromise.call(this);
        parse.call(this);
    }

    function createPromise(){
        this._promise = new Promise(resolver => {this._resolver = resolver;});
    }

    function parse(){

        const encoder = new TextEncoder();
        const instance = this;
        let arr = [];
        let currentByte = 0;
        let rowNumber = 1;
        Papa.parse(this._file, {
            header: true,
            encoding: 'utf-8',
            skipEmptyLines: 'greedy',
            step: function(row, parser){

                const country = row.data.country.trim();
                if(country && country.length > 0){

                    const name = row.data.name.trim();
                    if(name && name.length > 0){

                        if(instance._parser === undefined) instance._parser = parser;
                        const modifiedRow = country + ',' + name + ',' + (rowNumber++);
                        currentByte += (encoder.encode(modifiedRow).length);
                        arr.push(modifiedRow);
                        if(currentByte >= instance._chunkSize){
                            parser.pause();
                            instance._resolver({records: arr.join('\n'), lastRow: rowNumber - 1});
                            arr = [];
                            currentByte = 0;
                        }

                    }
                }

            },
            complete: function (){
                const lastResult = {isComplete: true}
                if(arr.length){
                    lastResult.records = arr.join('\n');
                    lastResult.lastRow = rowNumber - 1
                }
                instance._resolver(lastResult);
            }

        });

    }

    PausableCsvParser.prototype.getCurrentBatch = function(){
        return this._promise;
    }

    PausableCsvParser.prototype.prepareNextBatch = async function(){
        createPromise.call(this);
        this._parser.resume();
    }

    return PausableCsvParser;

})();

let parser = undefined;
onmessage = async(e) => {

    switch(e.data.type){

        case 'INIT':
            parser = new PausableCsvWorker(e.data.file, e.data.chunkSize);
            break;
        case 'GET':
            e.ports[0].postMessage(await parser.getCurrentBatch());
            parser.prepareNextBatch();
            break;

    }

}