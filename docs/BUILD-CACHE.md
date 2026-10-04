# Cache remota de Gradle

Este repo usa la misma cache remota de Gradle que `calorie-monitor-android-app`: un Reposilite en el
mini PC, desplegado con Coolify. El CI sube lo que compila en `main` y el resto de máquinas lo
descargan en vez de recompilarlo. Un solo servidor, una sola ruta (`/private/gradle-cache`) y los
mismos tokens para todos los proyectos Android: las claves de la cache son hashes de las entradas de
cada tarea, así que dos proyectos no se pisan.

**Montar el servidor, crear los tokens `ci`/`dev` y la limpieza mensual** se hace una vez y está en
[calorie-monitor-android-app · docs/BUILD-CACHE.md](https://github.com/amt911/calorie-monitor-android-app/blob/main/docs/BUILD-CACHE.md)
(§1–3). Aquí solo lo que es de este repo.

## Qué hay en el repo

- `settings.gradle.kts` declara la cache remota (`HttpBuildCache`, nativa de Gradle, sin plugins).
  No hace nada hasta que alguien le da una URL.
- `gradle.properties` activa la build cache (`org.gradle.caching=true`); sin eso Gradle ignora tanto
  la local como la remota.
- `.github/workflows/ci.yml` le pasa URL y credenciales al CI desde secrets, y solo sube en los push a
  `main` (los PR leen, nunca escriben). Sin los secrets se comporta exactamente como antes.

## El CI sube a la cache

En GitHub: repositorio → *Settings → Secrets and variables → Actions → New repository secret*, los
mismos tres valores que en `calorie-monitor-android-app`:

| Secret | Valor |
| --- | --- |
| `GRADLE_CACHE_URL` | `https://gradle-cache.<tu-dominio>/private/gradle-cache/` (la barra final cuenta) |
| `GRADLE_CACHE_USER` | `ci` |
| `GRADLE_CACHE_PASSWORD` | el `SERVICE_PASSWORD_CI` de Coolify |

El runner (`ubuntu-latest`, alojado por GitHub) tiene que alcanzar ese dominio: si solo resuelve en
tu red, el CI no puede subir y la cache se queda vacía para este repo.

## Las máquinas de desarrollo leen

Es configuración de la máquina, no del repo: si ya la pusiste para otro proyecto Android, este la usa
sin tocar nada. En `~/.gradle/gradle.properties` (el tuyo, **no** el del repo, que se commitea):

```properties
gradleCacheUrl=https://gradle-cache.<tu-dominio>/private/gradle-cache/
gradleCacheUser=dev
gradleCachePassword=<SERVICE_PASSWORD_DEV>
```

No pongas `gradleCachePush=true` fuera del CI: lo que sube una máquina con cambios a medias lo
descargan las demás. Las variables de entorno `GRADLE_CACHE_URL`, `GRADLE_CACHE_USER` y
`GRADLE_CACHE_PASSWORD` hacen lo mismo para una sesión concreta.

## Comprobar que funciona

Después de que el CI haya pasado al menos una vez por `main` con los secrets puestos:

```bash
rm -rf ~/.gradle/caches/build-cache-1   # solo la cache LOCAL: si no, los aciertos pueden venir de ahí
./gradlew --stop
./gradlew clean assembleDebug --console=plain | grep -c FROM-CACHE
```

Un número alto (decenas) es que está sirviendo. Si sale 0, el diagnóstico (401, 403, dominio que no
resuelve, `gradleCacheUrl` en el fichero equivocado) está en la guía de
`calorie-monitor-android-app`, §6.

La cache local sigue activa y se consulta primero: sin red o con el mini PC apagado, todo funciona
como siempre, solo que sin el atajo.
