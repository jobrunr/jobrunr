package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jobrunr.quarkus.autoconfigure.JobRunrProducer;
import org.jobrunr.quarkus.autoconfigure.JobRunrStarter;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrElasticSearchStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrInMemoryStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrMongoDBStorageProviderProducer;
import org.jobrunr.quarkus.autoconfigure.storage.JobRunrSqlStorageProviderProducer;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

class JobRunrExtensionProcessor {

    private static final String FEATURE = "jobrunr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem produce(Capabilities capabilities, CombinedIndexBuildItem index) {
        Set<Class> beanClasses = new HashSet<>();
        beanClasses.add(JobRunrProducer.class);
        beanClasses.add(JobRunrStarter.class);
        beanClasses.add(storageProviderClass(capabilities));
        beanClasses.add(jsonMapperClass(capabilities));

        System.out.println("========================================================");
        System.out.println(capabilities.getCapabilities());
        System.out.println(beanClasses);
        System.out.println(index.getIndex().getKnownClasses());

        System.out.println("========================================================");

        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(beanClasses.stream().map(Class::getName).collect(toSet()))
                .build();
    }

    private Class<?> jsonMapperClass(Capabilities capabilities) {
        if (capabilities.isPresent("io.quarkus.jsonb")) {
            return JobRunrProducer.JobRunrJsonBJsonMapperProducer.class;
        } else if (capabilities.isPresent("io.quarkus.jackson")) {
            return JobRunrProducer.JobRunrJacksonJsonMapperProducer.class;
        }
        throw new IllegalStateException("Either JSON-B or Jackson should be added via a Quarkus extension");
    }

    private Class<?> storageProviderClass(Capabilities capabilities) {
        if (capabilities.isPresent("io.quarkus.agroal")) {
            return JobRunrSqlStorageProviderProducer.class;
        } else if (capabilities.isPresent("io.quarkus.mongodb-client")) {
            return JobRunrMongoDBStorageProviderProducer.class;
        } else if (capabilities.isPresent("io.quarkus.elasticsearch-rest-high-level-client")) {
            return JobRunrElasticSearchStorageProviderProducer.class;
        } else {
            return JobRunrInMemoryStorageProviderProducer.class;
        }
    }
}
