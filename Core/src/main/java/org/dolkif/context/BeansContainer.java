package org.dolkif.context;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.dolkif.annotations.Qualify;
import org.dolkif.utils.beans.ClassUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public final class BeansContainer implements IBeansContainer {

    private final Set<Bean.Instance<?>> singletonInstances; //TODO provisional
    private final Set<Bean.Type<?>> prototypeTypes; //TODO provisional

    public BeansContainer(){
        singletonInstances = new HashSet<>();
        prototypeTypes = new HashSet<>();
    }

    @Override
    public boolean addBean(final @NonNull Bean.BeanBase<?> beanBase) {
        //TODO Revision requirements BeanBaseConfiguration Pattern Scope
        if(beanBase instanceof Bean.Instance<?> && !(((Bean.Instance<?> ) beanBase).getValue() instanceof Class<?>) ) // Singleton required obligatory Bean.Instace
            return singletonInstances.add((Bean.Instance<?>) beanBase);
        else if(beanBase instanceof Bean.Type<?>)
            return prototypeTypes.add((Bean.Type<?>) beanBase);
        //else if(((Bean.Instance<?> ) beanBase).getValue() instanceof Class<?>)
            //return prototypeTypes.add(new Bean.Type<>(beanBase.getConfiguration(),(Class<?>) beanBase.getValue(),((Bean.Type<?>) beanBase).getDependencies()));
        else
            return false;
    }

    @Override
    public List<Bean.BeanBase<?>> getAllBeans() {
        return Stream.concat(singletonInstances.stream(),prototypeTypes.stream()).toList();
    }

    @Override
    public <T> List<Bean.BeanBase<?>> filterBean(Bean.BeanReference<T> beanReference) {
        val optionalAnnotationQualify = (Qualify) Arrays.stream(beanReference.getAnnotationsLoaded())
                .filter(annotation -> annotation instanceof Qualify)
                .findFirst()
                .orElse(null);

        return getAllBeans()
                .stream()
                .filter(beans -> isTypeAvailable(beanReference.getClassType(),optionalAnnotationQualify).test(beans))
                .toList();
    }

    @Override
    public <T> Optional<Bean.BeanBase<?>> findBean(Bean.BeanReference<T> beanReference) {
        val optionalAnnotationQualify = (Qualify) Arrays.stream(beanReference.getAnnotationsLoaded())
                .filter(annotation -> annotation instanceof Qualify)
                .findFirst()
                .orElse(null);
        for (val bean: getAllBeans()) {
            if(isTypeAvailable(beanReference.getClassType(),optionalAnnotationQualify).test(bean))
                return Optional.of(bean);
        }
        return Optional.empty();
    }

    @Override
    public <T> List<Bean.BeanBase<?>> filterBean(Class<T> classType) {
        return getAllBeans()
                .stream()
                .filter(beanBase -> isTypeAvailable(classType,null).test(beanBase))
                .collect(Collectors.toList());
    }

    private Predicate<Bean.BeanBase<?>> isTypeAvailable(final @NonNull Class<?> classTypeFind, final Qualify qualifyClassTypeFind){
        Predicate<Bean.BeanBase<?>> beanBasePredicate = beanBase -> {
            if(beanBase instanceof Bean.Type<?>)
                return ClassUtils.findClassInheritanceAndInterfaceImplementations(classTypeFind,((Bean.Type<?>) beanBase).getValue()).isPresent();
            else if(beanBase instanceof Bean.Instance<?>)
                return ClassUtils.findClassInheritanceAndInterfaceImplementations(classTypeFind,((Bean.Instance<?>)beanBase).getValue().getClass()).isPresent();
            else
                return false;
        };

        return qualifyClassTypeFind == null
                ? beanBasePredicate
                : beanBasePredicate.and(beanBase -> beanBase.getConfiguration().getQualifierName() != null
                    && beanBase.getConfiguration().getQualifierName().equals(qualifyClassTypeFind.name()));

    }
}
