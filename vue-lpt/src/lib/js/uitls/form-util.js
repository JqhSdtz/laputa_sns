export function makeError(item, msg) {
    item.validateState = 'error';
    item.validateMessage = msg;
}

export function clearError(item) {
    item.validateState = 'success';
    item.validateMessage = '';
}