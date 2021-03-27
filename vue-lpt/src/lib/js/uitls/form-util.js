export function makeError(item, msg) {
    if (!item) return;
    item.validateState = 'error';
    item.validateMessage = msg;
}

export function clearError(item) {
    if (!item) return;
    item.validateState = 'success';
    item.validateMessage = '';
}