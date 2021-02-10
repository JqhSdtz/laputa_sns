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
    }
}