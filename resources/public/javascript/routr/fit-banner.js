(function() {
	function resizeFitBanners() {
		$(".fit-banner").each(function(index, element) {
			var width = $(element).parent().width();
			$(element).css("font-size", (width / 10) + "px");
			$(element).css("line-height", "normal");
		});
	}
	
	function enableHoverHighlights() {
		$(".hover-highlight").each(function(index, element) {
			var originalColor = $(element).css("color");
			$(element).hover(
			    function() { $(this).animate({ color: "#E24388" }, 200 ) },
			    function() { $(this).animate({ color: originalColor }, 1000 ) });
		});
	}
	
	$(document).ready(resizeFitBanners);
	$(document).ready(enableHoverHighlights);
	$(window).resize(resizeFitBanners);
})();