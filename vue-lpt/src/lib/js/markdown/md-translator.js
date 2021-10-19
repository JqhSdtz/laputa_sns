export function translateMd(opt) {
    // 将带有r-link的元素视为使用vue-router进行跳转的链接
    const rLinks = opt.el.getElementsByClassName('r-link');
    for (let i = 0; i < rLinks.length; ++i) {
        const rLink = rLinks[i];
        rLink.addEventListener('click', (event) => {
            const elem = event.target;
            const path = elem.getAttribute('href');
            if (path && opt.router) {
                opt.router.push({
                    path: path
                });
                event.preventDefault();
                return false;
            }
        });
    }
}