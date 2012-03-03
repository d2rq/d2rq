$(function(){
    $('h2').add('h3').each(function(index, element) {
        $(element).append('<small> <a class="section-anchor">#</a></small>').find('.section-anchor').attr('href', '#' + element.id);
    });
});
