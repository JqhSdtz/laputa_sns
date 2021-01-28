export function makeError(item, msg) {
    item.validateState = 'error';
    item.validateMessage = msg;
}