// GPL-3.0-or-later. App-to-page input only; no Java object is exposed to websites.
(() => {
  'use strict';
  if (window.__lunaReceiver) return;
  const original = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : () => [];
  let pad = null;
  const flags = [0x1000,0x2000,0x4000,0x8000,0x100,0x200,0,0,0x20,0x10,0x40,0x80,1,2,4,8,0x400];
  const emit = (name, value) => {
    const event = new Event(name);
    Object.defineProperty(event, 'gamepad', {value});
    window.dispatchEvent(event);
  };
  const pads = () => {
    const all = Array.from(original());
    if (pad) {
      // Keep ordinary Android controllers intact if one appears in our slot.
      if (all[pad.index]) {
        let next = all.findIndex(p => !p);
        if (next < 0) next = all.length;
        const old = pad;
        pad = Object.freeze({...pad, index:next});
        emit('gamepaddisconnected', Object.freeze({...old, connected:false}));
        emit('gamepadconnected', pad);
      }
      while (all.length <= pad.index) all.push(null);
      all[pad.index] = pad;
    }
    return all;
  };
  Object.defineProperty(navigator, 'getGamepads', {value:pads, configurable:true});
  Object.defineProperty(window, '__lunaReceiver', {value:(connected, mask=0, lt=0, rt=0, axes=[0,0,0,0]) => {
    if (!connected) {
      if (pad) {
        const old = pad; pad = null;
        emit('gamepaddisconnected', Object.freeze({...old, connected:false,
          buttons:Object.freeze(old.buttons.map(() => Object.freeze({pressed:false,touched:false,value:0}))),
          axes:Object.freeze([0,0,0,0])}));
      }
      return;
    }
    const first = !pad;
    const existing = Array.from(original());
    let index = pad ? pad.index : existing.findIndex(p => !p);
    if (index < 0) index=existing.length;
    const clamp=(v,min,max)=>Math.max(min,Math.min(max,Number.isFinite(v)?v:0));
    const buttons=flags.map((flag,i)=>{
      const value=i===6?clamp(lt,0,1):i===7?clamp(rt,0,1):((mask&flag)?1:0);
      return Object.freeze({pressed:value>0.5,touched:value>0,value});
    });
    pad=Object.freeze({id:'Xbox Wireless Controller (Luna USB receiver)',
      index, connected:true, mapping:'standard', timestamp:performance.now(),
      buttons:Object.freeze(buttons), axes:Object.freeze(axes.slice(0,4).map(v=>clamp(v,-1,1)))});
    if(first) emit('gamepadconnected',pad);
  }, configurable:false});
})();
