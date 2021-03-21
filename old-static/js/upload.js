if (typeof imgUrl === 'undefined')
    imgUrl = lpt.baseUrl + '/oss/com'
if (typeof uploadLmt === 'undefined')
    uploadLmt = 1;
let rawImg = null;

const uploader = new plupload.Uploader({
    runtimes: 'html5,flash,silverlight,html4',
    browse_button: 'selectfiles',
    //multi_selection: false,
    container: document.getElementById('container'),
    flash_swf_url: 'lib/plupload-2.1.2/js/Moxie.swf',
    silverlight_xap_url: 'lib/plupload-2.1.2/js/Moxie.xap',
    url: imgUrl,

    filters: {
        mime_types: [ //只允许上传图片和zip文件
            {title: "Image files", extensions: "jpg,gif,png,bmp"}
        ],
        max_file_size: '10mb', //最大只能上传10mb的文件
        prevent_duplicates: true //不允许选取重复文件
    },

    init: {
        PostInit: function () {
            document.getElementById('ossfile').innerHTML = '';
            document.getElementById('postfiles').onclick = function () {
                uploader.start();
                return false;
            };
        },

        FilesAdded: function (up, files) {
            let alerted = false;
            plupload.each(files, function (file) {
                if (up.files.length > uploadLmt) {
                    if (!alerted) {
                        alert('最多上传' + uploadLmt + '张图片');
                        alerted = true;
                    }
                    up.removeFile(file);
                    return;
                }
                document.getElementById('ossfile').innerHTML += '<div id="' + file.id + '">' + file.name + ' (' + plupload.formatSize(file.size) + ')<b></b>'
                    + '<div class="progress"><div class="progress-bar" style="width: 0%"></div></div>'
                    + '</div>';
            });
        },

        UploadProgress: function (up, file) {
            const d = document.getElementById(file.id);
            d.getElementsByTagName('b')[0].innerHTML = '<span>' + file.percent + "%</span>";
            const prog = d.getElementsByTagName('div')[0];
            const progBar = prog.getElementsByTagName('div')[0];
            progBar.style.width = 2 * file.percent + 'px';
            progBar.setAttribute('aria-valuenow', file.percent);
        },

        FileUploaded: function (up, file, info) {
            if (info.status == 200)
                document.getElementById(file.id).getElementsByTagName('b')[0].innerHTML = 'upload to oss success 回调服务器返回的内容是:' + info.response;
            const result = JSON.parse(info.response);
            if (result.state === 1) {
                if (rawImg === null)
                    rawImg = result.object;
                else
                    rawImg += '#' + result.object;
            } else
                alert(result.message);
        },

        Error: function (up, err) {
            if (err.code == -600)
                document.getElementById('console').appendChild(document.createTextNode("\n选择的文件太大了,可以根据应用情况，在upload.js 设置一下上传的最大大小"));
            else if (err.code == -601)
                document.getElementById('console').appendChild(document.createTextNode("\n选择的文件后缀不对,可以根据应用情况，在upload.js进行设置可允许的上传文件类型"));
            else if (err.code == -602)
                document.getElementById('console').appendChild(document.createTextNode("\n这个文件已经上传过一遍了"));
            else
                document.getElementById('console').appendChild(document.createTextNode("\nError xml:" + err.response));
        }
    }
});

uploader.init();
