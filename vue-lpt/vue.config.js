module.exports = {
    css: {
        loaderOptions: {
            less: {
                lessOptions: {
                    modifyVars: {
                        'overlay-background-color': 'rgba(0, 0, 0, 0.35)',
                        'action-sheet-header-height': '2rem'
                    },
                    javascriptEnabled: true
                }
            }
        }
    },
    pages: {
        lpt: {
            entry: './src/modules/lpt/lpt-main.js',
            template: 'public/lpt.html',
            title: 'laputa'
        },
        blog: {
            entry: './src/modules/blog/blog-main.js',
            template: 'public/blog.html',
            title: 'laputa-blog'
        }
    }
}