window.app = window.app || {};
window.app.jobProgressPercent = function(job){
      if(!job){
            return 0;
      }else if(!job.totalTests){
            job.progressPercent = 0;
            return 0;
      }
      with(job){
            if(state == 'READY'){
                job.progressPercent = 100;
            } else if(!totalTests){
                job.progressPercent = 0;
                return 0;
            }else{
                job.progressPercent = Math.floor(((failedTests + passedTests) *  100) / totalTests);
                return job.progressPercent
            }
      }
}

window.app.update = function(from, to){
    for (var attr in from) {
        if(from.hasOwnProperty(attr)){
            to[attr] = from[attr];
        }
    }
    return to;
}


window.addEventListener('polymer-ready', function(e) {

    PolymerExpressions.prototype.json = function(object) {
        return JSON.stringify(object, null, 2);
    }

    PolymerExpressions.prototype.removeNulls = function(object) {
         var isArray = object instanceof Array;
         for (var k in object){
            if (object[k]=== null) {
                isArray ? object.splice(k,1) : delete object[k];
            } else if (typeof object[k]=="object"){
                PolymerExpressions.prototype.removeNulls(object[k]);
            }
        }
        return object;
    }

    PolymerExpressions.prototype.filterKeys=  function(object){
        return Object.keys(object);
    }

    if (!Array.prototype.find) {
      Array.prototype.find = function(predicate) {
        if (this == null) {
          throw new TypeError('Array.prototype.find called on null or undefined');
        }
        if (typeof predicate !== 'function') {
          throw new TypeError('predicate must be a function');
        }
        var list = Object(this);
        var length = list.length >>> 0;
        var thisArg = arguments[1];
        var value;

        for (var i = 0; i < length; i++) {
          value = list[i];
          if (predicate.call(thisArg, value, i, list)) {
            return value;
          }
        }
        return undefined;
      };
    }
});


