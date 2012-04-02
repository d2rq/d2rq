function addAnchor(element, id) {
    $(element).append('<small>&nbsp;<a class="section-anchor">#</a></small>').find('.section-anchor').attr('href', '#' + id);
}
$(function(){
    $('h2').add('h3').add('h4').add('dt').add('.warning').filter('[id]').each(function(index, element) {
        addAnchor(element, element.id);
    });
    $('.properties th').filter('[id]').each(function(index, element) {
        addAnchor($(element).next(), element.id);
    });
});
