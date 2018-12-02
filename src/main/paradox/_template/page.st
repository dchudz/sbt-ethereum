<html>
  <head>
    <title>$page.title$</title>
    <link rel="stylesheet" type="text/css" href="$page.base$css/docs.css"/>
    <link href="https://fonts.googleapis.com/css?family=Share+Tech+Mono|Source+Sans+Pro:400,400i,700,700i" rel="stylesheet"/>
  </head>
  <body>
    <div id="navigationColumn">
      <div id="navTitle">
      	 <div id="navHomeLink" $if(page.home.active)$class="active"$endif$>$page.home.html$</div>
      	 <div id="navProjectVersion">v.$page.properties.("project.version")$</div>
	 <div class="retroRule">-=-=-=-=-=-</div>
      </div>	
      <div id="navigation">$page.navigation$</div>
    </div>
    <div id="page">
      <div id="pageContent">$page.content$</div>
    </div>
    <div id="rightColumn">
    </div>
    <div id="pageFooter">
      <b>&copy; 2018 Machinery For Change, Inc</b> &mdash;
      <i>This software provided <b>without warranty to the extent permitted by applicable law</b> under your choice of the <a href="http://www.gnu.org/licenses/lgpl-2.1.html">Lesser GNU Public License, v.2.1 (LGPL)</a> or
	the <a href="http://www.eclipse.org/legal/epl-v10.html">Eclipse Public License, v.1.0 (EPL)]</a></i>
    </div>
  </body>
</html>