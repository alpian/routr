(function() {
    function initialize() {
        var map = new google.maps.Map(
            document.getElementById("map-canvas"), 
            {
                center: routrMapCenter,
                zoom: 13
            });
        var lastPoint = new google.maps.Marker({
            position: routrMapCenter,
            map: map,
            title: routrLastTime
        });
        var infowindow = new google.maps.InfoWindow({
            content: "Point recorded at " + routrLastTime
        });
        google.maps.event.addListener(lastPoint, 'click', function() {
            infowindow.open(map, lastPoint);
        });
        
        var flightPath = new google.maps.Polyline({
            map: map,
            path: routrTrail,
            geodesic: true,
            strokeColor: '#00EE22',
            strokeOpacity: 0.8,
            strokeWeight: 3
        });

    }
    google.maps.event.addDomListener(window, 'load', initialize);
})();
