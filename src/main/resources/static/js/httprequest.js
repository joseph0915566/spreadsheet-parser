import {axios} from "/js/third-party/axios.min.js";

export const HttpClient = (() => {

    function HttpClient(baseUrl, timeout){
        this._client = axios.create({
            baseURL: baseUrl,
            timeout: timeout
        })
    }

    function _doRequest(configs){

        return this._client.request({
            url: configs.url,
            params: configs.params,
            method: configs.method,
            data: configs.payload,
            onUploadProgress: configs.uploadProgressListener
        });

    }

    HttpClient.prototype.doRequest = function(configs){
        return _doRequest.call(this, configs);
    }

    return HttpClient;

})();
