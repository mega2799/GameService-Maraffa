package generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotation for required fields in Swagger schema. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Required {
}
