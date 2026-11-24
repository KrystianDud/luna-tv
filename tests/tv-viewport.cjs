const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const observers=[];
const head={metas:[],querySelectorAll(){return this.metas;},appendChild(x){this.metas.push(x);}};
const document={head:null,createElement(){return {name:'',content:''};}};
const window={};window.top=window;
vm.runInNewContext(fs.readFileSync('app/src/main/assets/tv-viewport.js','utf8'),{
 window,document,screen:{width:960},MutationObserver:class {
  constructor(callback){this.callback=callback;observers.push(this);}
  observe(){} disconnect(){this.disconnected=true;}
 }});
document.head=head;observers[0].callback();
assert.equal(head.metas[0].content,'width=1280, initial-scale=0.75, minimum-scale=0.75');
head.metas.push({name:'viewport',content:'width=device-width, initial-scale=1, maximum-scale=1'});
observers[1].callback();
assert.ok(head.metas.every(m=>m.content===head.metas[0].content));
head.metas[1].content='width=device-width';observers[1].callback();
assert.equal(head.metas[1].content,head.metas[0].content);
assert.equal(observers[0].disconnected,true);
console.log('TV viewport: early document, later viewport insertion and framework updates passed.');
