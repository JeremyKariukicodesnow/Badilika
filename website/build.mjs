import {readFileSync,writeFileSync} from 'node:fs';
import {pages} from './pages.mjs';
const home=readFileSync('index.html','utf8');
const header=home.match(/<header>[\s\S]*?<\/header>/)[0];
const footer=home.match(/<footer>[\s\S]*?<\/footer>/)[0];
for(const [route,page] of Object.entries(pages)){
  const head=home.match(/<head>[\s\S]*?<\/head>/)[0].replace(/<title>.*?<\/title>/,'<title>'+page.title+'</title>').replace(/name="description" content="[^"]*"/,'name="description" content="'+page.description+'"').replace('href="https://badilika.sparkfield.co.ke/"','href="https://badilika.sparkfield.co.ke/'+route+'"').replace(/property="og:title" content="[^"]*"/,'property="og:title" content="'+page.title+'"').replace(/property="og:description" content="[^"]*"/,'property="og:description" content="'+page.description+'"');
  writeFileSync(route+'.html','<!doctype html><html lang="en">'+head+'<body class="inner-page"><div class="page-progress" aria-hidden="true"></div><a class="skip" href="#main">Skip to content</a>'+header.replace('href="#download"','href="/#download"')+'<main id="main"><section class="route-hero"><div><p class="eyebrow">'+page.eyebrow+'</p><h1>'+page.heading+'</h1><p class="intro">'+page.intro+'</p></div></section>'+page.body+'<section class="route-end"><h2>Your next small step.</h2><a class="button dark-button" href="/downloads/Badilika-1.8.apk" download>Download Badilika ↓</a><a class="text-link dark" href="/features">Explore all features ↗</a></section></main>'+footer+'</body></html>');
}
const routes=['','features','protection','guide','rules','privacy','terms'];
writeFileSync('sitemap.xml','<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'+routes.map(route=>'<url><loc>https://badilika.sparkfield.co.ke/'+route+'</loc></url>').join('')+'</urlset>\n');
