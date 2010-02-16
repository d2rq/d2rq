function showAllMetadata(name) {
	var ele = document.getElementById(name);
	if (ele == null) return;
	var tables = document.getElementsByTagName('table');
	for (i = 0; i < tables.length; i++) {
		tables[i].style.display = 'block';
	}
}