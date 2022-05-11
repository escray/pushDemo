package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<T> implements Provider<T>, ContextConfig.ComponentProvider<T> {
    private final ContextConfig context;
    private Class<?> componentType;
    private Constructor<T> injectConstructor;
    private boolean constructive = false;

    public ConstructorInjectionProvider(ContextConfig context, Class<?> componentType, Constructor<T> injectConstructor) {
        this.context = context;
        this.componentType = componentType;
        this.injectConstructor = injectConstructor;
    }

    @Override
    public T get() {
        return get(context.getContext());
    }

    @Override
    public T get(Context context) {
        if (constructive) throw new CyclicDependenciesFoundException(componentType);
        try {
            constructive = true;
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(p -> context.get(p.getType())
                            .orElseThrow(() -> new DependencyNotFoundException(componentType, p.getType())))
                    .toArray(Object[]::new);
            //.orElseThrow(DependencyNotFoundException::new))

            return injectConstructor.newInstance(dependencies);
        } catch (CyclicDependenciesFoundException e) {
            throw new CyclicDependenciesFoundException(componentType, e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            constructive = false;
        }
    }
}
