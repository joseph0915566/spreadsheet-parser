export const Log = {};
Log.info = message => {console.info(message);}
Log.warn = message => {console.warn(message);}
Log.err = (message, err) => {console.error(message + ' ===> ' + err.stack);}