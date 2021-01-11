/**
 * Wizards javascript
 */


/**
 * Type ahead - https://twitter.github.io/typeahead.js/examples/
 * for docker hub - https://hub.docker.com/api/content/v1/products/search?page_size=50&q=busybox&type=image
 */

// templates
var template = Handlebars.compile($("#result-template").html());
var empty = Handlebars.compile($("#empty-template").html());

// Init the element dubbed Image
$('#Image').addClass('typeahead');

// Bloodhound - 
var engine = new Bloodhound({
	  datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name', 'slug'),
	  queryTokenizer: Bloodhound.tokenizers.whitespace,
	  
	  remote: {
		url: url + '&op=search&q=%QUERY', 
	    wildcard: '%QUERY'
	  } 
	}); 

var engine1 = new Bloodhound({
	  datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name', 'slug'),
	  queryTokenizer: Bloodhound.tokenizers.whitespace,
	  remote: {
		url: url + '&op=search&q=%QUERY&source=community', 
	    wildcard: '%QUERY'
	  } 
}); 

// Image must be enclosed by a div called image-box
$('#image-box .typeahead').typeahead(null, 
		{
		  name: 'publisher',
		  display: 'name',
		  //displayKey: 'name',
		  source: engine,
		  templates: {
			
			suggestion: template
			//empty: empty 
		  }  
		},
		{
		  name: 'community',
		  display: 'name',
		  source: engine1,
		  templates: {
			suggestion: template,
			empty: empty 
		  }  
		}
);
