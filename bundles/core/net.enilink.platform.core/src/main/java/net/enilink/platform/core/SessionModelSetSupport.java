package net.enilink.platform.core;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.enilink.composition.annotations.Precedes;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.traits.Behaviour;
import net.enilink.platform.core.security.ISecureModelSet;
import net.enilink.platform.core.security.SecurePropertySetFactory;
import net.enilink.platform.core.security.SecurityUtil;
import net.enilink.komma.common.adapter.AdapterSet;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.adapter.IAdapterSet;
import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;

import com.google.inject.Inject;

@Precedes(IModelSet.class)
public abstract class SessionModelSetSupport implements IModelSet.Internal,
		Behaviour<IModelSet> {
	public static class Key {
	};

	private EntityVar<Map<Object, IAdapterSet>> sessionScopedAdapterSets;

	@Inject
	protected IContextProvider contextProvider;

	private ComposedAdapterFactory createAdapterFactory() {
		return new ComposedAdapterFactory(
				ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE) {
			/**
			 * Default adapter factory for all namespaces
			 */
			class DefaultItemProviderAdapterFactory extends
					ReflectiveItemProviderAdapterFactory {
				public DefaultItemProviderAdapterFactory() {
					super(KommaEditPlugin.getPlugin());
				}

				@Override
				public Object adapt(Object object, Object type) {
					if (object instanceof IClass) {
						// do not override the adapter for classes
						return null;
					}
					return super.adapt(object, type);
				}

				public boolean isFactoryForType(Object type) {
					// support any namespace
					return type instanceof URI || supportedTypes.contains(type);
				}
			}

			DefaultItemProviderAdapterFactory defaultAdapterFactory;
			{
				defaultAdapterFactory = new DefaultItemProviderAdapterFactory();
				defaultAdapterFactory.setParentAdapterFactory(this);
			}

			@Override
			protected IAdapterFactory getDefaultAdapterFactory(Object type) {
				// provide a default adapter factory as fallback if no
				// specific adapter factory was found
				return defaultAdapterFactory;
			}
		};
	}

	@Override
	public IAdapterSet adapters() {
		// use key to make adapter set session scoped
		IContext context = contextProvider.get();
		ISession session = context.getSession();
		Object key = session.getAttribute(Key.class.getName());
		// fixes an error if parts of platform where reloaded via OSGi
		if (!(key instanceof Key)) {
			key = new Key();
			session.setAttribute(Key.class.getName(), key);
		}

		Map<Object, IAdapterSet> adapterSets = sessionScopedAdapterSets.get();
		if (adapterSets == null) {
			synchronized (sessionScopedAdapterSets) {
				adapterSets = sessionScopedAdapterSets.get();
				if (adapterSets == null) {
					adapterSets = Collections.synchronizedMap(new WeakHashMap<>());
					sessionScopedAdapterSets.set(adapterSets);
				}
			}
		}

		IAdapterSet adapterSet = adapterSets.get(key);
		if (adapterSet == null) {
			synchronized (adapterSets) {
				adapterSet = adapterSets.get(key);
				if (adapterSet == null) {
					adapterSet = new AdapterSet(getBehaviourDelegate());
					adapterSets.put(key, adapterSet);
					initializeAdapters();
				}
			}
		}
		return adapterSet;
	}

	protected void initializeAdapters() {
		// Create the command stack that will notify this editor as commands
		// are executed.
		EditingDomainCommandStack commandStack = new EditingDomainCommandStack();
		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(
				createAdapterFactory(), commandStack, getBehaviourDelegate()) {
			public boolean isReadOnly(IModel model) {
				URI user = SecurityUtil.getUser();
				if (user != null
						&& model.getModelSet() instanceof ISecureModelSet
						&& ((ISecureModelSet) model.getModelSet())
								.writeModeFor((IReference) model, user) == null) {
					return true;
				}
				return super.isReadOnly(model);
			}
		};
		commandStack.setEditingDomain(editingDomain);
		// editingDomain
		// .setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
	}

	@Override
	public Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
		return SecurePropertySetFactory.class;
	}
}