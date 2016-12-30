# [ZingChart](http://www.zingchart.com)
Build v2.1.0

For more info on using ZingChart, see the docs: http://www.zingchart.com/docs

##Quick Start
Include a reference to the zingchart library
```
<script src="zingchart.min.js"></script>
```
The `zingchart` object is now accessible. Happy charting!
```
<div id="chart"></div>
<script>
window.onload = function() {
zingchart.render({
    id: "chart",
    data: {
        type: "line",
        series: [{ values: [5,10,15,5,10,5] }]
    }
    });
};
</script>
```


##Package Directory
The package includes the following:
```
|   README.md
|   zingchart.min.js
├── modules
│   ├── zingchart-3d.min.js
│   ├── zingchart-animation.min.js
│   ├── zingchart-api-annotations.min.js
│   ├── ...
├── phantomjs
│   ├── zingchart-phantomjs.min.js
│   ├── modules-phantomjs
''	'''
