$(document).ready(function() {
    $("#search").focus(function() {
        $(this).attr("value", "");
        $("#searchResult").slideUp();
    });

    $("#search").change(function() {
        $("#searchResult").html("Searching");
        $.getJSON("http://ws.geonames.org/searchJSON?q=" + encodeURIComponent($(this).attr("value")) + "&maxRows=10&style=SHORT&lang=de&country=DE&callback=?",
            function(data){
                $("#searchResult").html("").slideDown();
                if (data.totalResultsCount == 0) {
                    $("#searchResult").html("No results found.").slideDown();
                }
                $.each(data.geonames, function(i,item){
                    var div = $("<div/>").appendTo($("#searchResult"));
                    $("<a/>").text(item.name).attr("href", "javascript:center("+item.lng+","+item.lat+",15)").appendTo(div);
                });
            });

    });
});