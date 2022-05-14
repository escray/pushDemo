package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class InjectionTest {

    ContextConfig config;
    Dependency dependency = new Dependency() {};

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
        config.bind(Dependency.class, dependency);
    }

    @Nested
    // 构造函数注入
    public class ConstructorInjection {
        // DONE: No args constructor
        // 无依赖的组件应该通过默认构造函数生成组件实例
        @Test
        public void should_bind_type_to_a_class_with_default_constructor() {

            Component instance = getComponent(Component.class, ComponentWithDefaultConstructor.class);

            assertNotNull(instance);
            assertTrue(instance instanceof ComponentWithDefaultConstructor);
        }

        // DONE: with dependencies
        // 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
        @Test
        public void should_bind_type_to_a_class_with_inject_constructor() {



            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);
            assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
        }

        // DONE: A -> B -> C
        // 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
        @Test
        public void should_bind_type_to_a_class_with_transitive_dependencies() {

            config.bind(Dependency.class, DependencyWithInjectConstructor.class);
            config.bind(String.class, "indirect dependency");

            Component instance = getComponent(Component.class, ComponentWithInjectConstructor.class);

            assertNotNull(instance);

            Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
            assertNotNull(dependency);

            assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
        }

        // sad path, error condition

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {

            }
        }

        // TODO: abstract class
        @Test
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class));
        }

        // TODO: interface
        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(Component.class));
        }

        // DONE: multi inject constructors
        // 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
        @Test
        public void should_throw_exception_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructors.class));
        }

        // DONE: no default constructor and inject constructor
        // 如果组件没有 Inject 标注的构造函数，也没有默认构造函数（新增任务）
        @Test
        public void should_throw_exception_if_no_inject_nor_default_constructor_provider() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class));
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider =
                    new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }
    }

    private <T, R extends T> T getComponent(Class<T> type, Class<R> implementation) {
        config.bind(type, implementation);
        return config.getContext().get(type).get();
    }

    @Nested
    public class FieldInjection {

        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
        }

        // TODO: inject field
        @Test
        public void should_inject_dependency_via_field() {
            ComponentWithFieldInjection component = getComponent(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {
            SubclassWithFieldInjection component = getComponent(SubclassWithFieldInjection.class, SubclassWithFieldInjection.class);
            assertSame(dependency, component.dependency);
        }

        // TODO: throw exception if field is final
        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
        }

        // TODO: provider dependency information for field injection
        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<ComponentWithFieldInjection> provider =
                    new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray(Class<?>[]::new));
        }
    }

    @Nested
    public class MethodInjection {
        static class InjectMethodWithNoDependency {
            boolean called = false;

            @Inject
            void install() {
                this.called = true;
            }
        }

        // TODO: inject method with no dependencies will be called

        @Test
        public void should_call_inject_method_even_if_no_dependency_declared() {
            InjectMethodWithNoDependency component = getComponent(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
            assertTrue(component.called);
        }

        static class InjectMethodWithDependency {
            Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        // TODO: inject method with dependencies will be injected

        @Test
        public void should_inject_dependency_via_inject_method() {
            InjectMethodWithDependency component = getComponent(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
            assertSame(dependency, component.dependency);
        }

        // TODO: override inject method from superclass
        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                this.superCalled++;
            }
        }

        static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                this.subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            SubclassWithInjectMethod component = getComponent(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);

            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);
        }

        static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_inject_method_with_inject() {
            SubclassOverrideSuperClassWithInject component = getComponent(SubclassOverrideSuperClassWithInject.class, SubclassOverrideSuperClassWithInject.class);
            assertEquals(1, component.superCalled);
        }

        static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() {
            SubclassOverrideSuperClassWithNoInject component = getComponent(SubclassOverrideSuperClassWithNoInject.class, SubclassOverrideSuperClassWithNoInject.class);

            assertEquals(0, component.superCalled);
        }

        // TODO: include dependencies from inject method
        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<InjectMethodWithDependency> provider
                    = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class},
                    provider.getDependencies().toArray(Class<?>[]::new));
        }

        // TODO: throw exception if type parameter defined
        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class,
                    () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
        }
    }
}
