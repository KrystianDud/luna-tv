// Tests the actual browser adapter, including standard mapping, disconnection and coexistence.
const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict');
const events=[], native=[null,null,null,null];
const context={navigator:{getGamepads:()=>native},performance:{now:()=>123},Event:class{constructor(type){this.type=type;}},
 window:{dispatchEvent:e=>events.push(e)}};
vm.createContext(context);
vm.runInContext(fs.readFileSync('app/src/main/assets/receiver-gamepad.js','utf8'),context);
const update=context.window.__lunaReceiver, get=context.navigator.getGamepads;
assert.equal(get().filter(Boolean).length,0);
update(true,0x1000|0x20|1,0.25,1,[0.5,-1,-0.25,0]);
let pad=get()[0];
assert.equal(pad.mapping,'standard'); assert.equal(pad.buttons.length,17);
assert.equal(pad.buttons[0].pressed,true); assert.equal(pad.buttons[1].pressed,false);
assert.equal(pad.buttons[6].value,0.25); assert.equal(pad.buttons[6].pressed,false);
assert.equal(pad.buttons[7].pressed,true); assert.equal(pad.buttons[8].pressed,true);
assert.equal(pad.buttons[12].pressed,true); assert.equal(pad.axes[1],-1);
assert.equal(events.filter(e=>e.type==='gamepadconnected').length,1);
update(true,0,0,0,[0,0,0,0]); assert.equal(get()[0].buttons[0].pressed,false);
assert.equal(events.filter(e=>e.type==='gamepadconnected').length,1);
native[0]={id:'ordinary Android controller',index:0};
assert.equal(get()[0].id,native[0].id); assert.equal(get()[1].index,1);
update(false); assert.equal(get()[1],null); assert.equal(get()[0].id,native[0].id);
assert.equal(events.at(-1).gamepad.connected,false);
assert.ok(events.at(-1).gamepad.buttons.every(b=>!b.pressed));
update(true,0,99,-99,[9,-9,NaN,0]);
pad=get()[1]; assert.equal(pad.buttons[6].value,1); assert.equal(pad.buttons[7].value,0);
assert.equal(pad.axes[0],1); assert.equal(pad.axes[1],-1); assert.equal(pad.axes[2],0);
for(const [flag,index] of [[0x1000,0],[0x2000,1],[0x4000,2],[0x8000,3],[0x100,4],[0x200,5],
 [0x20,8],[0x10,9],[0x40,10],[0x80,11],[1,12],[2,13],[4,14],[8,15],[0x400,16]]) {
 update(true,flag); pad=get()[1];
 assert.equal(pad.buttons.filter(b=>b.pressed).length,1);
 assert.equal(pad.buttons[index].pressed,true);
}
console.log('Receiver browser adapter: mapping, analog ranges, button release, disconnect and native coexistence passed.');
