package com.agmente.di;

import com.agmente.data.SessionStorage;
import com.agmente.data.db.AgmenteDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideSessionStorageFactory implements Factory<SessionStorage> {
  private final Provider<AgmenteDatabase> databaseProvider;

  public AppModule_ProvideSessionStorageFactory(Provider<AgmenteDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SessionStorage get() {
    return provideSessionStorage(databaseProvider.get());
  }

  public static AppModule_ProvideSessionStorageFactory create(
      Provider<AgmenteDatabase> databaseProvider) {
    return new AppModule_ProvideSessionStorageFactory(databaseProvider);
  }

  public static SessionStorage provideSessionStorage(AgmenteDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSessionStorage(database));
  }
}
