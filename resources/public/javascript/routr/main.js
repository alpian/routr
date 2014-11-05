(function() {
  function initialize() {
    var map = new google.maps.Map(
      document.getElementById("map-canvas"), 
      {
        center: {lat: 46.189683, lng: 6.776343},
        zoom: 13,
        mapTypeId: google.maps.MapTypeId.TERRAIN
      });
  }
  google.maps.event.addDomListener(window, 'load', initialize);
})();
