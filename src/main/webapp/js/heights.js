var heights;

function printHeightGraph() {
    var runs = (kilometers.length + 1)/20;
    heights = new Array();
    for (i=0;i<runs;i++)
    {
        var tStart = startPoint.transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));
        getHeight(tStart, 0);
        getHeight(tStart, distance);
        for (j=0;j<20;j++) {
            if (i*20 + j < kilometers.length) {
                var tKilometer = kilometers[i*20 + j].transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));
                getHeight(tKilometer, i*20 + j+1);
            }
        }
    }
}

function getHeight(point, kilometer) {
    $.getJSON("http://ws.geonames.org/srtm3JSON?callback=?", {
        lng: point.x,
        lat: point.y
        }, function (data) {
        addNewData(data, kilometer)
    });
}

function Numsort (a, b) {
    return a[0]-b[0];
}

function addNewData(data, kilometer) {
    heights.push([kilometer, data.srtm3]);
    heights.sort(Numsort);
    $.plot($("#ch_heights"), [ heights ]);
}