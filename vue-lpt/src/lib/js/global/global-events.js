function getNameArr(_name) {
    let names = [];
    if (typeof _name === 'string') {
        names.push(_name);
    } else if (typeof _name === 'object') {
        names = _name;
    }
    return names;
}

export function createEventBus() {
    const eventMap = new Map();
    return {
        on(name, fun) {
            const names = getNameArr(name);
            names.forEach(_name => {
                let funArr = eventMap.get(_name);
                if (!funArr) {
                    funArr = new Array();
                    eventMap.set(_name, funArr);
                }
                funArr.push(fun);
            });
        },
        emit(name, param) {
            const names = getNameArr(name);
            names.forEach(_name => {
                const funArr = eventMap.get(_name);
                if (!funArr)
                    return;
                funArr.forEach(fun => {
                    if (typeof fun === 'function')
                        fun(param, name);
                });
            });
        },
        off(name, fun) {
            const names = getNameArr(name);
            names.forEach(_name => {
                const funArr = eventMap.get(_name);
                funArr.forEach((_fun, idx) => {
                    if (_fun === fun)
                        funArr.splice(idx, 1);
                });
            });
        }
    };
}

const globalEvent = createEventBus();

export default globalEvent;