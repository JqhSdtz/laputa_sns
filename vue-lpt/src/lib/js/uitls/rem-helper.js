const pRem = 16;
let initialed = false;

const option = {
    // PC端设计宽度
    pcWidth: 1024,
    // 移动端设计宽度
    mobWidth: 414,
    // 移动端的最大宽度，超过此宽度视为PC
    maxMobWidth: 1000,
    responsive: false
}

function initRem(param) {
    Object.assign(option, param);
    const html = document.getElementsByTagName('html')[0];
    const oriWidth = document.body.clientWidth || document.documentElement.clientWidth;
    const width = oriWidth > option.maxMobWidth ? option.pcWidth : option.mobWidth;
    html.style.fontSize = oriWidth / width * pRem + 'px';
    if (!initialed) {
        initialed = true;
        if (option.responsive) {
            window.addEventListener('resize', initRem);
        }
    }
}

function remToPx(value) {
    const oriWidth = document.body.clientWidth || document.documentElement.clientWidth;
    const width = oriWidth > option.maxMobWidth ? option.pcWidth : option.mobWidth;
    return oriWidth / width * pRem * value;
}

function getRem() {
    return document.getElementsByTagName('html')[0].style.fontSize;
}

export default {
    initRem,
    getRem,
    remToPx
}