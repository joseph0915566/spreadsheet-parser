import {Log} from "/js/log.js"

export const RetryableLimiter = (function (){

    const DELAY = 'DELAY';

    function RetryableLimiter(taskGenerator, limit, retryCount, retryDelay){
        this._taskGenerator = taskGenerator;
        this._inflightLimit = limit;
        this._retryCount = retryCount;
        this._retryDelay = retryDelay;
        this._inflightQueue = new Set();
        this._retryQueue = [];
    }

    async function* taskConsumer(){

        let inflightCount = 0;
        while(true){

            while(inflightCount < this._inflightLimit){
                const retryableTask = this._retryQueue.length ?
                                        this._retryQueue.pop() :
                                        {
                                            executable: (await this._taskGenerator.next()).value,
                                            retryDelay: this._retryDelay,
                                            retriesLeft: this._retryCount
                                        };
                if(retryableTask === undefined || retryableTask.executable === undefined){
                    if (this._inflightQueue.size) break;
                    else return undefined;
                }
                let inflightItem = (async() => retryableTask.executable())().then(success => [inflightItem, success], err => [inflightItem, err, retryableTask]);
                this._inflightQueue.add(inflightItem);
                inflightCount++;
            }

            if(this._inflightQueue.size){
                let [inflightItem, payload, inflightTask] = await Promise.race(this._inflightQueue);
                this._inflightQueue.delete(inflightItem);
                if(inflightTask === undefined){
                    inflightCount--;
                    yield [payload, inflightTask];
                }
                else{

                    if(payload === DELAY){
                        Log.info('Inserting task for retry ' + JSON.stringify(inflightTask));
                        this._retryQueue.push(inflightTask);
                    }
                    else{
                        inflightCount--;
                        yield [payload, inflightTask];
                    }

                }

            }

        }

    }

    RetryableLimiter.prototype.result = function(){
        return taskConsumer.call(this);
    }

    RetryableLimiter.prototype.retryTask = function(task){
        let inflightItem = new Promise(resolve => setTimeout(resolve, task.retryDelay)).then(success => [inflightItem, DELAY, task], err => [inflightItem, err, task]);
        this._inflightQueue.add(inflightItem);
    }

    return RetryableLimiter;

})();
