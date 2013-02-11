package java.lang.annotation;

@Documented
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.ANNOTATION_TYPE)

public @interface Retention
{
  RetentionPolicy value();
}
