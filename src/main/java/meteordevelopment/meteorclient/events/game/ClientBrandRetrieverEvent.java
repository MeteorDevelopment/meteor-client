package meteordevelopment.meteorclient.events.game;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.events.Cancellable;

public class ClientBrandRetrieverEvent extends Cancellable {
	private static final ClientBrandRetrieverEvent INSTANCE = new ClientBrandRetrieverEvent();
	
	public CallbackInfoReturnable<String> info;
	

    public static ClientBrandRetrieverEvent get(CallbackInfoReturnable<String> info) {
    	ClientBrandRetrieverEvent event = INSTANCE;
    	
    	event.setCancelled(false);
    	event.info = info;
    	
    	return event;
    }
}

