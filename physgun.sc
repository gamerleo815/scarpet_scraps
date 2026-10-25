__command() -> (
    run('give @s stick[minecraft:enchantment_glint_override=1b,minecraft:custom_name="Physgun",minecraft:custom_data={\'physgun\':1b}]')  
);

__on_start() -> (
    global_grabbing = false;
    global_grabbed_entity = null;
    global_entity_invulnerable = false;

    global_vfx = true;
    global_sfx = true;
    global_color = 1308622847;

    global_grab_time = time();
);

toggle_vfx() -> (
    global_vfx = !global_vfx
);
toggle_sfx() -> (
    global_sfx = !global_sfx
);

set_color(r,g,b,a) -> (
    global_color = _scformat(r/255,g/255,b/255,a/255)
);

// color API
_int(num) -> if (num < 0, ceil(num), floor(num));

global_INT2HEX = ['0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'];

_hex(num) -> if (num % 1 == 0,
	result = '';
	if (num < 0,
		num += 1;
		loop (1, result = get(global_INT2HEX, 16 + num % 16) + result; num = _int(num / 16));
		result = 'F' + result;
	,
		loop (2, result = get(global_INT2HEX, num % 16) + result; num = _int(num / 16))
	);
	result
,
	'invalid number'
);

_reverse_str(string) -> (
    new='';
    for(range(length(string)),
        ch = slice(string,_,_+1);
        new = ch + new;
    );
    new
);

_from_hex(hex) -> (
    result = 0;
    hex = _reverse_str(hex);
    for(range(length(hex)),
        ch = slice(hex,_,_+1);
        result += (global_INT2HEX~ch)*(16^_);
    );
    result
);

_scformat(...RGBA_color) -> (
    result = '';
    for(RGBA_color,result+=str(_hex(floor(_*255))));
    _from_hex(result);
);

// picking up mobs
__on_player_interacts_with_entity(player, entity, hand) -> (
    item = query(player,'holds',hand);
    if(item:2:'components':'minecraft:custom_data':'physgun',
        // if we are not currently grabbing an entity, then grab the entity we're looking at
        if(!global_grabbing,
            __grab(entity)
        );
        'cancel'
    )
);

// launching entities, gravity gun style
__on_player_attacks_entity(player, entity) -> (
    item = query(player(),'holds');
    if(item:2:'components':'minecraft:custom_data':'physgun' && global_grabbing,
        global_grabbing = false;
        modify(global_grabbed_entity,'invulnerable',global_entity_invulnerable); // reset invulnerability to previous state
        if(global_sfx,sound('block.respawn_anchor.deplete',player()~'pos',1,1+(rand(0.08)-0.04))); // play a dope throw sound
        modify(global_grabbed_entity,'motion',(player()~'look') * 2);
        'cancel'
    )
);

// activation
__grab(entity) -> (
    global_grabbing = true;
    global_grabbed_entity = entity;
    global_entity_invulnerable = entity~'invulnerable'; // get it's invulnerability to reset it later
    global_grab_time = time(); // debounce
    modify(entity,'invulnerable',true); // make it invulnerable so it doesn't die
    if(global_sfx,sound('block.beacon.activate',player()~'pos',1,1+(rand(0.08)-0.04))) // play a dope activate sound
);
// deactivation
__drop() -> (
    global_grabbing = false;
    modify(global_grabbed_entity,'invulnerable',global_entity_invulnerable); // reset invulnerability to previous state
    if(global_sfx,sound('block.beacon.deactivate',player()~'pos',1,1+(rand(0.08)-0.04))) // play a dope deactivate sound
);

// makes it so you can let go of the entity without looking directly at it
__on_player_uses_item(player, item_tuple, hand) -> (
    if(global_grabbing && (time() - global_grab_time > 1) && (item_tuple:2:'components':'minecraft:custom_data':'physgun'),
        __drop()
    )
);

// holding logic
__on_tick() -> (
    if(global_grabbing,
        if(global_grabbed_entity~'health' <= 0,global_grabbing = false;return());
        height = global_grabbed_entity~'height';
        // teleport entity
        //modify(global_grabbed_entity,'pos',);
        // set motion to move towards target position
        
        modify(global_grabbed_entity,'motion',(((player()~'look')*3 + (player()~'pos') + [0,0.5+height/2,0]) - global_grabbed_entity~'pos')*0.8);
        // draw effects

        if(global_vfx,
            width = (global_grabbed_entity~'width'/2);
            draw_shape('box',1,{
                'from' -> global_grabbed_entity~'pos' - [width,0,width],
                'to' -> global_grabbed_entity~'pos' + [width,height,width],
                'color' -> global_color
            });
            draw_shape('line',1,{
                'from' -> global_grabbed_entity~'pos' + [0,height/2,0],
                'to' -> player()~'pos' + [0,player()~'height'/2,0],
                'color' -> global_color
            })
        )
    )
)