export const Queue = (function (){

    function Queue(){
        this._elements = {};
        this._head = 0;
        this._tail = 0;
    }

    function insert(element){
        this._elements[this._tail++] = element;
    }

    function getFirst(){
        const item = this._elements[this._head];
        delete this._elements[this._head++];
        return item;
    }

    function getSize(){
        return this._tail - this._head;
    }

    Queue.prototype.push = function(element){
        insert.call(this, element);
    }

    Queue.prototype.pop = function(){
        return getFirst.call(this);
    }

    Queue.prototype.size = function(){
        return getSize.call(this);
    }

    return Queue;

})();