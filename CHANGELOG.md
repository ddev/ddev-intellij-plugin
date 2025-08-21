# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),

## [1.2.7]

### Changed
* Fix incorrect versions in CHANGELOG.md by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/413
* Update the GitHub URLs after the move to DDEV org & replace contact info by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/422
* ci: move secrets to 1password, fixes #421 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/426
* ci: fix missing secret checks for sonarcloud workflow by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/429
* fix: update plugin dependency checks and notifications, fixes #402 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/428
* feat: update version detection to support ddev head version by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/433
* fix: resolve SonarQube warnings by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/458
* fix: remove problematic manual database introspection causing ref count mismatch by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/476
* feat: adapt timeouts for ddev command calls by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/479
* fix: resolve EDT violation in process creation by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/480
* feat: run manual database introspection only when adding the ddev connection by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/481
* fix: handle race condition in widget initialization by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/483
* feat: add experimental ddev executor for composer by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/482

### Dependency Updates
* Bump org.sonarqube from 6.0.1.5171 to 6.1.0.5360 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/418
* Bump org.junit.platform:junit-platform-launcher from 1.11.3 to 1.12.2 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/417
* Bump org.mockito:mockito-core from 5.16.0 to 5.17.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/416
* Bump getsentry/action-release from 2 to 3 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/419
* Bump io.sentry:sentry from 8.8.0 to 8.9.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/420
* Bump com.google.code.gson:gson from 2.13.0 to 2.13.1 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/425
* Bump io.sentry:sentry from 8.9.0 to 8.10.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/427
* Bump io.sentry:sentry from 8.10.0 to 8.11.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/430
* build: bump io.sentry:sentry from 8.11.1 to 8.12.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/436
* build: bump org.sonarqube from 6.1.0.5360 to 6.2.0.5505 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/438
* build: bump org.mockito:mockito-core from 5.17.0 to 5.18.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/440
* build: bump junitVersion from 5.12.2 to 5.13.2 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/452
* build: bump io.sentry:sentry from 8.12.0 to 8.15.1 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/455
* build: bump org.junit.platform:junit-platform-launcher from 1.12.2 to 1.13.2 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/453
* build: bump io.sentry:sentry from 8.15.1 to 8.16.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/456
* build: bump io.sentry:sentry from 8.16.0 to 8.17.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/461
* build: bump org.junit.platform:junit-platform-launcher from 1.13.2 to 1.13.3 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/460
* build: bump junitVersion from 5.13.2 to 5.13.3 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/459
* build: bump org.junit.platform:junit-platform-launcher from 1.13.3 to 1.13.4 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/462
* build: bump io.sentry:sentry from 8.17.0 to 8.18.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/465
* build: bump org.jetbrains.changelog from 2.2.1 to 2.4.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/467
* build: bump org.mockito:mockito-core from 5.18.0 to 5.19.0 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/475
* build: bump actions/checkout from 4 to 5 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/474
* build: bump 1password/load-secrets-action from 2 to 3 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/473
* build: bump io.sentry:sentry from 8.18.0 to 8.19.1 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/471
* build: bump org.assertj:assertj-core from 3.27.3 to 3.27.4 by @dependabot[bot] in https://github.com/ddev/ddev-intellij-plugin/pull/468

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.6...1.2.7

## [1.2.6]

### Changed
- Update build for 2025.1 support by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410

### Dependency Updates
- Update Gradle Wrapper to 8.13 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/411
- Bump JUnit 5 from 5.11.2 to 5.12.2 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump com.google.code.gson:gson from 2.11.0 to 2.13.0 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump getsentry/action-release from 1 to 2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/407
- Bump intellij-plugin-verifier from 1.307 to 1.384 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump io.sentry:sentry from 7.18.0 to 8.8.0 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump org.assertj:assertj-core from 3.26.3 to 3.27.3 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump org.jetbrains.intellij.platform from 2.1.0 to 2.3.0 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump org.mockito:mockito-core from 5.14.2 to 5.16.0 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410
- Bump org.sonarqube from 5.1.0.4882 to 6.0.1.5171 by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/410

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.5...1.2.6

## [1.2.5]

### Changed
- Update build for 2024.3 support by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/385
- Upgrade for 2024.3 compatibility by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/388

### Fixed
- Fix COMPOSE_PROJECT_NAME period issue #367 by @edditor in https://github.com/ddev/ddev-intellij-plugin/pull/374

### Dependency Updates
- Bump org.jetbrains.intellij.platform from 2.0.1 to 2.1.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/369
- Bump junitVersion from 5.11.0 to 5.11.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/375
- Bump org.mockito:mockito-core from 5.12.0 to 5.14.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/378
- Bump org.junit.platform:junit-platform-launcher from 1.11.0 to 1.11.3 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/382
- Bump io.sentry:sentry from 7.14.0 to 7.18.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/386

### New Contributors
- @edditor made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/374

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.4...1.2.5

## [1.2.4]

### Changed
- Update build for 2024.2 support by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/350
- Change autoconfiguration of node.js binary path to node by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/358

### Fixed
- Fix deprecated functionality in workflow by @Cheoooon in https://github.com/ddev/ddev-intellij-plugin/pull/325
- Fix redirect limit in update check by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/351
- Fix visibility change in statusbar by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/352
- Prevent ProgressIndicator warnings by using an EmptyProgressIndicator by @AkibaAT in https://github.com/ddev/ddev-intellij-plugin/pull/359

### Dependency Updates
- Bump junitVersion from 5.10.2 to 5.10.3 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/332
- Bump gradle/actions from 3 to 4 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/345
- Bump io.sentry:sentry from 7.9.0 to 7.14.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/348
- Bump org.junit.platform:junit-platform-launcher from 1.10.2 to 1.11.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/349
- Bump org.sonarqube from 5.0.0.4638 to 5.1.0.4882 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/336
- Bump org.jetbrains.changelog from 2.2.0 to 2.2.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/355
- Bump junitVersion from 5.10.2 to 5.11.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/356
- Bump org.assertj:assertj-core from 3.26.0 to 3.26.3 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/354

### New Contributors
- @AkibaAT made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/350

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.3...1.2.4

## [1.2.3]

### Changed

- Fix Plugin verifier task by @Cheoooon in https://github.com/ddev/ddev-intellij-plugin/pull/317
- Upgrade Intellij gradle plugin v2 by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/324
- Downgrade plugin verifier by @Cheoooon and @nico-loeber

### Dependency Updates

- Bump org.sonarqube from 4.4.1.3373 to 5.0.0.4638 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/314
- Bump gradle/wrapper-validation-action from 2.1.2 to 3.3.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/316
- Bump org.mockito:mockito-core from 5.10.0 to 5.11.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/315
- Project name in env variable must be in lowercase by @SchmidtWebmedia
  in https://github.com/ddev/ddev-intellij-plugin/pull/323
- Bump io.sentry:sentry from 7.8.0 to 7.9.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/319
- Bump org.mockito:mockito-core from 5.11.0 to 5.12.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/320
- Bump com.google.code.gson:gson from 2.10.1 to 2.11.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/321
- Bump org.assertj:assertj-core from 3.25.3 to 3.26.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/322

### New Contributors

- @Cheoooon made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/317
- @SchmidtWebmedia made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/323

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.2...1.2.3

## [1.2.2]

### Known issues

- Errors like `There is no ProgressIndicator or Job in this thread, the current job is not cancellable.` will appear in
  the error log
- Plugin verifier task fails due to insufficient disk space on the GitHub runner

### Changed

- Update version support 241 by @Aneida in https://github.com/ddev/ddev-intellij-plugin/pull/308

### Dependency Updates

- Bump org.jetbrains.intellij from 1.17.0 to 1.17.3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/306
- Bump org.assertj:assertj-core from 3.25.2 to 3.25.3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/292
- Bump gradle/wrapper-validation-action from 1.1.0 to 2.1.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/305
- Bump softprops/action-gh-release from 1 to 2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/302
- Bump org.junit.platform:junit-platform-launcher from 1.10.1 to 1.10.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/290
- Bump junitVersion from 5.10.1 to 5.10.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/291
- Bump io.sentry:sentry from 7.3.0 to 7.7.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/309
- Bump io.sentry:sentry from 7.7.0 to 7.8.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/310

### New Contributors

- @Aneida made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/308

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.1...1.2.2

## [1.2.1]

### Changed

- Show context in error message when docker backend is not running

### Fixed

- Fix mysql driver selection by @mwaltonen in https://github.com/ddev/ddev-intellij-plugin/pull/283
- fix(ci): Correct regex of version tags by @pan93412 in https://github.com/ddev/ddev-intellij-plugin/pull/244

### Dependency Updates

- Bump org.jetbrains.intellij from 1.16.1 to 1.17.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/279
- Bump actions/cache from 3 to 4 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/280
- Bump org.assertj:assertj-core from 3.25.1 to 3.25.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/282
- Bump gradle/gradle-build-action from 2 to 3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/285
- Bump peter-evans/create-or-update-project-card from 2 to 3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/286
- Bump org.mockito:mockito-core from 5.8.0 to 5.10.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/281
- Bump io.sentry:sentry from 7.1.0 to 7.3.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/287
- Bump the github_actions group across 1 directories with 1 update by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/289

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.2.0...1.2.1

## [1.2.0]

### Changed

- Add silverstripe, plural upload_dirs by @Firesphere in https://github.com/ddev/ddev-intellij-plugin/pull/221
- Fix display issue of the ddev version in the update notification by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/222
- Bump io.sentry:sentry from 6.27.0 to 6.28.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/219
- Add highlighting for ddev commands that can be executed in ui by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/223
- refactor(settings): Use isEmpty instead of equals("") by @pan93412
  in https://github.com/ddev/ddev-intellij-plugin/pull/238
- refactor(cmd): Call nonModal() instead of NON_MODAL constant by @pan93412
  in https://github.com/ddev/ddev-intellij-plugin/pull/239
- refactor(icons): Use the ClassLoader overload of getIcon by @pan93412
  in https://github.com/ddev/ddev-intellij-plugin/pull/241
- Add support for mailpit service by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/243
- Remove schema for ddev config by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/269
- Add index for generated configurations by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/260
- Add support of EAP build (233.*) by @pan93412 in https://github.com/ddev/ddev-intellij-plugin/pull/242
- Add support for IntelliJ 2023.3 Update by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/270

### Dependency Updates

- Bump org.mockito:mockito-core from 5.4.0 to 5.5.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/224
- Bump actions/checkout from 3 to 4 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/230
- Bump org.jetbrains.changelog from 2.1.2 to 2.2.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/225
- Bump io.sentry:sentry from 6.28.0 to 6.30.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/236
- Bump org.sonarqube from 4.3.0.3225 to 4.4.1.3373 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/237
- Bump org.mockito:mockito-core from 5.5.0 to 5.7.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/252
- Bump junitVersion from 5.10.0 to 5.10.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/254
- Bump org.jetbrains.intellij from 1.15.0 to 1.16.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/246
- Bump org.junit.platform:junit-platform-launcher from 1.10.0 to 1.10.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/253
- Bump io.sentry:sentry from 6.30.0 to 6.33.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/255
- Bump org.mockito:mockito-core from 5.7.0 to 5.8.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/265
- Bump org.jetbrains.intellij from 1.16.0 to 1.16.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/266
- Bump actions/setup-java from 3 to 4 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/267
- Bump actions/upload-artifact from 3 to 4 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/271
- Bump org.assertj:assertj-core from 3.24.2 to 3.25.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/275
- Bump io.sentry:sentry from 6.33.1 to 7.1.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/272

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.1.1...1.2.0

## [1.1.1]

### Changed

- Update the plugin for compatibility to IntelliJ 2023.2 by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/216
- Update the plugin for compatibility to ddev 1.22 by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/215
- Update ddev version check for better performance by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/210
- Replace drud namespace by ddev by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/217

### Fixed

- Fix Docker.exe not found error by @nico-loeber with kind support of @bitExpert
  in https://github.com/ddev/ddev-intellij-plugin/pull/214

### Dependency Updates

- Bump org.sonarqube from 4.0.0.2929 to 4.2.0.3129 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/186
- Bump io.sentry:sentry from 6.19.0 to 6.21.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/185
- Bump org.jetbrains.changelog from 2.0.0 to 2.1.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/201
- Bump org.jetbrains.intellij from 1.13.3 to 1.15.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/199
- Bump org.mockito:mockito-core from 5.3.1 to 5.4.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/195
- Bump io.sentry:sentry from 6.21.0 to 6.26.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/208
- Bump org.sonarqube from 4.2.0.3129 to 4.3.0.3225 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/205
- Bump org.junit.platform:junit-platform-launcher from 1.9.3 to 1.10.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/213
- Bump junitVersion from 5.9.3 to 5.10.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/212
- Bump io.sentry:sentry from 6.26.0 to 6.27.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/211

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.1.0...1.1.1

## [1.1.0]

### Added

- Auto Configuration for NodeJS Remote Interpreter by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/177
- Add craftcms as valid project type to the DDEV configuration schema

### Fixed

- Support mutagen enabled projects by placing a project specific interpreter path mapping
- Do not replace unchanged data source configuration by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/180

### Dependency Updates

- Bump junitVersion from 5.9.2 to 5.9.3 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/173
- Bump org.mockito:mockito-core from 5.2.0 to 5.3.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/172
- Bump io.sentry:sentry from 6.17.0 to 6.18.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/176
- Bump org.junit.platform:junit-platform-launcher from 1.9.2 to 1.9.3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/174
- Bump io.sentry:sentry from 6.18.1 to 6.19.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/179

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.5...1.1.0

## [1.0.5]

### Fixed

- Compatibility of the DDEV status bar for IntelliJ 2023.1
- Compatibility of the DDEV terminal for IntelliJ 2023.1
- Compatibility of actions for IntelliJ 2023.1

### Dependency Updates

- Update gradle wrapper to 8.0
- Bump mockito-core from 4.9.0 to 4.10.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/137
- Bump mockito-core from 4.10.0 to 4.11.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/140
- Bump sentry from 6.9.2 to 6.11.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/142
- Bump gson from 2.10 to 2.10.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/145
- Bump org.jetbrains.intellij from 1.10.1 to 1.12.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/149
- Bump junit-platform-launcher from 1.9.1 to 1.9.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/147
- Bump org.mockito:mockito-core from 4.11.0 to 5.1.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/154
- Bump junitVersion from 5.9.1 to 5.9.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/146
- Bump assertj-core from 3.23.1 to 3.24.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/150
- Bump org.jetbrains.intellij from 1.12.0 to 1.13.2 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/165
- Bump org.mockito:mockito-core from 5.1.1 to 5.2.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/164
- Bump org.sonarqube from 3.5.0.2730 to 4.0.0.2929 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/160
- Bump io.sentry:sentry from 6.11.0 to 6.17.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/169
- Bump org.jetbrains.intellij from 1.13.2 to 1.13.3 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/168

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.4...1.0.5

## [1.0.4]

### Fixed

- Configure DataSource TreePattern to index all schemas by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/135
- Fix failing unit tests by adding `junit-platform-launcher` as dependency by @SpraxDev
  in https://github.com/ddev/ddev-intellij-plugin/pull/131
- GitHub Actions: Upload test reports even when tests fail by @SpraxDev
  in https://github.com/ddev/ddev-intellij-plugin/pull/132

### Dependency Updates

- Bump sentry from 6.5.0 to 6.6.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/118
- Bump sentry from 6.6.0 to 6.9.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/130
- Bump mockito-core from 4.8.1 to 4.9.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/125
- Bump org.sonarqube from 3.4.0.2513 to 3.5.0.2730 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/119
- Bump gson from 2.9.1 to 2.10 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/117
- Bump org.jetbrains.changelog from 1.3.1 to 2.0.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/120
- Bump org.jetbrains.intellij from 1.10.0 to 1.10.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/134
- Bump sentry from 6.9.1 to 6.9.2 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/133

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.3...v1.0.4

## [1.0.3]

### Fixed

* Fix mariadb specific dsn type by @nico-loeber in https://github.com/ddev/ddev-intellij-plugin/pull/107
* Create docker remote server if it does not exist by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/102
* Re-enable test coverage report by @SpraxDev in https://github.com/ddev/ddev-intellij-plugin/pull/88

### Dependency Updates

* Bump gson from 2.9.0 to 2.9.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/90
* Bump org.jetbrains.intellij from 1.7.0 to 1.8.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/93
* Bump sentry from 6.3.0 to 6.3.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/91
* Bump org.jetbrains.intellij from 1.8.0 to 1.8.1 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/99
* Bump sentry from 6.3.1 to 6.4.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/96
* Bump mockito-core from 4.6.1 to 4.7.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/94
* Bump sentry from 6.4.0 to 6.4.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/101
* Bump org.jetbrains.intellij from 1.8.1 to 1.9.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/103
* Bump mockito-core from 4.7.0 to 4.8.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/105
* Bump sentry from 6.4.1 to 6.4.3 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/111
* Bump junitVersion from 5.9.0 to 5.9.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/109
* Bump sentry from 6.4.3 to 6.5.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/112
* Bump mockito-core from 4.8.0 to 4.8.1 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/116

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.1...v1.0.2

## [1.0.2]

## [1.0.1]

### Changed

- Support for IntelliJ 2022.2 (No more support for 2022.1)
- Increase ddev command timeout and add path to error message
- Bump org.jetbrains.intellij from 1.6.0 to 1.7.0 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/84
- Bump sentry from 6.1.4 to 6.3.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/87
- Bump junitVersion from 5.8.2 to 5.9.0 by @dependabot in https://github.com/ddev/ddev-intellij-plugin/pull/89

### Fixed

- Add missing checkout step for sentry release job
- Fix possible null pointer in Description::getServices

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.0...v1.0.1

## [1.0.0]

### Added

- Add deployment configuration for the ddev mount by @nico-loeber
  in https://github.com/ddev/ddev-intellij-plugin/pull/57
- Add option to disable version check in update notification

### Changed

- Refactor dynamic ddev config arguments for better extensibility
- Update DdevNotifier and Tests for better maintainability
- Bump sentry from 5.7.4 to 6.1.4 by @dependabot
- Bump org.sonarqube from 3.3 to 3.4.0.2513 by @dependabot
  in https://github.com/ddev/ddev-intellij-plugin/pull/71
- Remove dead code and do some tidy up

### Fixed

- Fix changelog format so the changelog is shown correctly in JetBrains Marketplace
- Remove unsupported JVM option MaxPermSize
- Do not record uncaught exceptions in sentry.io

**Please note**: There is a known issue
where [DDEV commands fail when the project is opened in PHPStorm](https://github.com/ddev/ddev-intellij-plugin/issues/80)
.

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.0-beta1...v1.0.0

## [1.0.0-beta1]

### Added

- Make DDEV binary configurable and refactor command execution to use system environment
  in https://github.com/ddev/ddev-intellij-plugin/pull/66
- Add sentry.io error reporting (#37) by @SpraxDev in https://github.com/ddev/ddev-intellij-plugin/pull/60
- Add check if docker is running on startup in https://github.com/ddev/ddev-intellij-plugin/pull/68
- Add PHP Interpreter automatically for composer
- Add notification if track of the DDEV status is lost
- Add Got It Tutorial for the integrated DDEV terminal
- Add validation for plugins that are required for PHP interpreter registration
- Reload plugin on configuration change. A restart of the IDE is no longer required.

### Changed

- Parse DDEV JSON output per line in https://github.com/ddev/ddev-intellij-plugin/pull/61
- Enable Start / Stop / Restart buttons when in DDEV status is unknown to restore the status
- Enable dynamic loading of the plugin -> No restart needed when enabling / disabling the plugin
- Various updates of the dependencies

### Fixed

- Fix error when DDEV status was not parsable by the plugin
- Fix error when the output of DDEV contained multiple JSON objects
- Fix timeout when using zsh with enabled git plugin in WSL as default shell
- Fix wrong logging channel for some classes
- Fix error when opening multiple projects with running DDEV container at the same time
- Minor wording/typo fixes for DdevIntegrationBundle.properties, for #48 by @rfay
  in https://github.com/ddev/ddev-intellij-plugin/pull/51
- Various optimizations and fixes

### New Contributors

- @dependabot made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/46
- @rfay made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/51
- @SpraxDev made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/60

**Full Changelog**: https://github.com/ddev/ddev-intellij-plugin/compare/1.0.0-alpha3...1.0.0-beta

## [1.0.0-beta]

Broken because of an error in the release pipeline.

## [1.0.0-alpha3]

### Added

- Ddev PHP Interpreter will automatically be set as default interpreter for the project if none is set yet
- Added tests for ServiceActionManager by @kevin0x90
  in [#29](https://github.com/ddev/ddev-intellij-plugin/pull/29)
- Added first interactive tutorial element for the ddev status
  bar [#5](https://github.com/ddev/ddev-intellij-plugin/issues/5)

### Changed

- Improved error handling and extended timeouts for ddev background processes to
  address [#23](https://github.com/ddev/ddev-intellij-plugin/issues/23)
- General optimizations

### Fixed

- Fixed warning in
  WSL `your 131072x1 screen size is bogus` [#30](https://github.com/ddev/ddev-intellij-plugin/issues/30)
- Fixed [#25](https://github.com/ddev/ddev-intellij-plugin/issues/25) by adding a notification if a soft
  dependency is missing
- Fixed [#24](https://github.com/ddev/ddev-intellij-plugin/issues/24) by connecting to database via 127.0.0.1
  instead of localhost

### New Contributors

* @kevin0x90 made their first contribution in https://github.com/ddev/ddev-intellij-plugin/pull/29

### Special Thanks

Thanks to our alpha testers of the alpha2 pre release!

Your feedback made this release possible:
[@marcvangend](https://github.com/marcvangend), [@calien666](https://github.com/calien666)
, [@cmuench](https://github.com/cmuench), [@kgaut](https://github.com/kgaut) and others

## [1.0.0-alpha2]

### Added

- Assistance and completion for DDEV configuration file with included

### Changed

- General Refactorings
- Supported IntelliJ Versions set 2022.1

### Fixed

- Auto Configuration for PHP Remote Interpreter works much better now
- Lots of Bug fixes

## [1.0.0-alpha1]

### Added

- Run DDEV actions via GUI
- Quick access DDEV Services
- Auto configure PHP Remote Interpreter
- Auto configure data source
- Assistance and completion for DDEV configuration file
- DDEV version check
- Integrated DDEV terminal
