# WASLNI (وصلني) — Project Worklog

> Shared work log for all agents working on the WASLNI delivery navigation app.
> Append-only. Each new section starts with `---`.

---
Task ID: phase-1
Agent: main
Task: Phase 1 — Planning: Produce complete reference documentation before any code.

Work Log:
- Read and analyzed the full PRD (100 sections) and execution order from the user.
- Asked 8 clarifying questions; user pinned: App=WASLNI, Package=com.waslni.driver, Min SDK=24, Map=Mapbox, Auth=Username+Password, Backend=FastAPI+PostgreSQL (parallel with Android), Architecture=Clean+MVVM, Code delivery=In-chat, First phase=Planning.
- Created /home/z/my-project/waslni/docs/ directory.
- Wrote 01-PRD.md — Product Requirements Document with MVP scope, use cases, user flows, UI specs, error handling, acceptance criteria, risks.
- Wrote 02-architecture.md — Clean Architecture + MVVM, Android stack, Backend stack, Mapbox provider abstraction, sync architecture, security, battery strategy.
- Wrote 03-database.md — PostgreSQL schema (7 tables: users, customers, deliveries, refresh_tokens, audit_logs, idempotency_keys) + Room schema (3 entities) + DAOs + SQLAlchemy models + Alembic plan.
- Wrote 04-api-contract.md — Full OpenAPI-style contract: 4 auth endpoints, 5 customer endpoints, 4 delivery endpoints, 1 sync endpoint. Unified error model, idempotency, conflict resolution, rate limiting, Pydantic schemas, Retrofit interfaces.
- Wrote 05-project-structure.md — Monorepo layout, Android package structure (core/data/domain/di/presentation), backend layout, Git strategy (Conventional Commits + branching), CI/CD pipelines (GitHub Actions), versioning, code review guidelines.
- Wrote 06-build-phases.md — 32-phase roadmap from Planning to MVP Launch with dependencies, acceptance criteria, timeline estimate, milestones.

Stage Summary:
- Phase 1 (Planning) complete.
- 6 reference documents produced in /home/z/my-project/waslni/docs/:
  - 01-PRD.md
  - 02-architecture.md
  - 03-database.md
  - 04-api-contract.md
  - 05-project-structure.md
  - 06-build-phases.md
- Next: Phase 2 (Android Foundation) — create Android project skeleton with Compose + Hilt + Navigation + Theme + RTL.
- Critical decisions locked: Mapbox for maps, FastAPI parallel with Android, Offline-first with Room as source of read, latest-write-wins for customer conflicts, Idempotency-Key for delivery completion.

---
Task ID: phase-2
Agent: main
Task: Phase 2 — Android Foundation: Create Android project skeleton with Compose + Hilt + Navigation + Theme + RTL + placeholder screens.

Work Log:
- Created /home/z/my-project/waslni/android/ directory tree.
- Wrote gradle/libs.versions.toml — version catalog with all dependencies (Compose BOM, Hilt, Room, Retrofit, OkHttp, kotlinx.serialization, WorkManager, DataStore, Security Crypto, Play Services Location, Mapbox Maps + Navigation, full testing stack).
- Wrote settings.gradle.kts — configured Mapbox Maven repo with auth.
- Wrote build.gradle.kts (root) + gradle.properties.
- Wrote app/build.gradle.kts — namespace com.waslni.driver, minSdk=24, targetSdk=34, Java 17, Compose enabled, BuildConfig for Mapbox token + API_BASE_URL (dev=10.0.2.2, prod=api.waslni.com).
- Wrote app/proguard-rules.pro — keep rules for Hilt, Room, Retrofit, kotlinx.serialization, Mapbox, coroutines.
- Wrote AndroidManifest.xml — permissions (INTERNET, LOCATION, POST_NOTIFICATIONS, FOREGROUND_SERVICE_LOCATION, VIBRATE), WaselApp as application name, MainActivity as launcher, WorkManager initializer disabled (Hilt-managed).
- Wrote WaselApp.kt — @HiltAndroidApp, Configuration.Provider for HiltWorkerFactory.
- Wrote MainActivity.kt — @AndroidEntryPoint, installSplashScreen, enableEdgeToEdge, hosts WaselNavHost inside WaselniTheme.
- Wrote core/ui/theme/Color.kt — brand palette (teal primary, orange secondary, yellow accent), Light + Dark semantic colors, map marker colors.
- Wrote core/ui/theme/Type.kt — Material 3 typography with larger body text for in-motion readability.
- Wrote core/ui/theme/Shape.kt — rounded shapes for friendly feel.
- Wrote core/ui/theme/Theme.kt — WaselniTheme() wrapper with light/dark color schemes, edge-to-edge window setup.
- Wrote presentation/navigation/Routes.kt — all route constants + BottomNavDestination enum with icons.
- Wrote presentation/navigation/WaselNavHost.kt — NavHost with splash/login/home/customers/history/settings/add-customer, Scaffold with bottom bar that hides on detail screens.
- Wrote presentation/auth/splash/SplashScreen.kt — 800ms delay then navigates to login (TODO Phase 11: real session check).
- Wrote presentation/auth/login/LoginScreen.kt — username + password fields with visibility toggle, login button, loading state.
- Wrote presentation/home/HomeScreen.kt — search bar + map placeholder + Add Customer FAB.
- Wrote presentation/customers/CustomerListScreen.kt — search bar + empty state + Add FAB.
- Wrote presentation/customers/AddCustomerScreen.kt — TopAppBar with back, name/phone/location fields, save button.
- Wrote presentation/delivery/HistoryScreen.kt — empty state.
- Wrote presentation/settings/SettingsScreen.kt — placeholder.
- Wrote di/AppModule.kt — provides WorkManager (Phase 3+ will add Database/Network/Repository modules).
- Wrote res/values/strings.xml (English default) + res/values-ar/strings.xml (Arabic) — app name, nav labels, login, customer fields, common actions.
- Wrote res/values/themes.xml — Theme.Waselni + Theme.Waselni.Starting (SplashScreen API).
- Wrote res/values/colors.xml — brand + background colors for splash.
- Wrote res/values/ic_launcher_background.xml.
- Wrote res/drawable/ic_splash_logo.xml — vector location pin for splash.
- Wrote res/drawable/ic_launcher_foreground.xml — vector for adaptive icon.
- Wrote res/mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml — adaptive icons.
- Wrote res/xml/backup_rules.xml + data_extraction_rules.xml — exclude encrypted prefs + Room DB from cloud backup.
- Wrote android/README.md + local.properties.example + .gitignore.

Stage Summary:
- Phase 2 (Android Foundation) complete.
- 16 Kotlin files + 12 XML resource files + 5 Gradle/config files produced.
- App package: com.waslni.driver, applicationId: com.waslni.driver.debug (debug variant).
- Theme: Material 3, Dark/Light, Arabic RTL with system Sans font.
- Navigation: single-activity, NavHost with 7 routes, bottom nav on 4 main destinations.
- Next: Phase 3 (Local Data) — Room database with CustomerEntity, DeliveryEntity, SyncOperationEntity + DAOs + mappers + domain models + repository interfaces.

---
Task ID: phase-3
Agent: main
Task: Phase 3 — Local Data (Room): Build the complete local-first data layer with entities, DAOs, repositories, use cases, and Hilt wiring.

Work Log:
- Created directory tree: domain/{model,repository,usecase/customer}, data/local/{dao,entity,mapper}, data/repository, test/data/local/dao, test/domain/model.
- Wrote domain/model/Customer.kt — pure Kotlin data class with init-block validation (name 2-120, phone 7-30, lat -90..90, lng -180..180, accuracy >=0).
- Wrote domain/model/LatLng.kt — coordinate pair with Haversine distanceTo() for arrival detection.
- Wrote domain/model/LocationResult.kt — GPS fix result with isAcceptable(threshold) and DEFAULT_THRESHOLD=10m.
- Wrote domain/model/DeliveryStatus.kt — full state machine with isTerminal, isActive, allowedNextStates, canTransitionTo. Prevents illegal jumps like PENDING→DELIVERED.
- Wrote domain/model/Delivery.kt — delivery entity with isStateConsistent() that validates timestamp/status invariants.
- Wrote domain/model/SyncEnums.kt — SyncState (SYNCED/PENDING/FAILED) and SyncOperation (CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, CREATE_DELIVERY, UPDATE_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY).
- Wrote domain/model/SyncOperation.kt — sync queue item with idempotencyKey for COMPLETE_DELIVERY.
- Wrote data/local/entity/{CustomerEntity,DeliveryEntity,SyncOperationEntity}.kt — Room entities with indexes (phone, name, syncState, customerId, status, createdAt). FK RESTRICT on deliveries→customers.
- Wrote data/local/dao/CustomerDao.kt — observeAll, search (name OR phone, NOCASE), observeById, observeCount, observePendingSyncCount, getById, getByPhone, insert, update, delete, updateSyncState, bulkUpdateSyncState.
- Wrote data/local/dao/DeliveryDao.kt — observeAll, observeByDateRange, observeByStatus, observeByCustomer, observeById, observeCountByStatusSince, getActiveForCustomer, markStarted, markArrived, markCompleted, markCancelled.
- Wrote data/local/dao/SyncOperationDao.kt — getPending (oldest first), getPendingBatch, observePendingCount, incrementRetry (auto-FAILED at maxRetries via SQL CASE), cleanOldSynced, deletePendingForEntity.
- Wrote data/local/WaselDatabase.kt — @Database with 3 entities, version=1, exportSchema=true.
- Wrote data/local/WaselConverters.kt — placeholder for future type converters.
- Wrote data/local/mapper/{CustomerMapper,DeliveryMapper,SyncOperationMapper}.kt — bidirectional toDomain()/toEntity() with syncState handling.
- Wrote domain/repository/{CustomerRepository,DeliveryRepository,SyncRepository}.kt — interfaces with Flow for reactive reads and suspend for writes.
- Wrote data/repository/CustomerRepositoryImpl.kt — Offline-first: writes go to Room + enqueue SyncOperation. Duplicate phone detection. Active delivery check on delete.
- Wrote data/repository/DeliveryRepositoryImpl.kt — State machine enforcement via canTransitionTo. Auto-generates idempotencyKey for COMPLETE_DELIVERY.
- Wrote data/repository/SyncRepositoryImpl.kt — wraps SyncOperationDao with domain model mapping.
- Wrote 8 customer use cases: AddCustomerUseCase, UpdateCustomerUseCase, UpdateCustomerLocationUseCase, DeleteCustomerUseCase, ObserveCustomersUseCase, SearchCustomersUseCase, GetCustomerUseCase, CheckDuplicatePhoneUseCase.
- Wrote di/DatabaseModule.kt — provides WaselDatabase (singleton) + 3 DAOs.
- Wrote di/RepositoryModule.kt — @Binds for 3 repository interfaces.
- Wrote di/UseCaseModule.kt — @Provides for 8 use cases.
- Wrote di/SerializationModule.kt — provides kotlinx.serialization Json instance (ignoreUnknownKeys, encodeDefaults).
- Wrote 4 test files (39 test methods total):
  * CustomerDaoTest — 12 tests covering insert, getById, observeAll (sort), search (name/phone/empty), update, delete, getByPhone, observeCount, updateSyncState, getBySyncState, observePendingSyncCount.
  * DeliveryDaoTest — 9 tests covering insert, getActiveForCustomer, markCompleted, markCancelled, observeByDateRange, observeByStatus, observeCountByStatusSince, FK constraint violation.
  * SyncOperationDaoTest — 8 tests covering getPending (ordering), observePendingCount, updateStatus, markFailed, incrementRetry (with auto-FAILED), cleanOldSynced (preserves PENDING), getLatestForEntity, getPendingBatch.
  * DomainModelsTest — 18 tests covering Customer validation, DeliveryStatus state machine, LatLng.distanceTo Haversine, LocationResult.isAcceptable threshold logic.

Stage Summary:
- Phase 3 (Local Data) complete.
- 32 new Kotlin files (28 main + 4 test) added on top of Phase 2's 16.
- Total project: 52 Kotlin main files + 4 test files.
- Layered architecture now functional:
  * domain/model — pure Kotlin (no Android deps), validation in init blocks
  * data/local — Room layer (entities, DAOs, mappers, database)
  * data/repository — Offline-first implementations with sync queue wiring
  * domain/usecase — 8 customer use cases
  * di — 5 Hilt modules (App, Database, Repository, UseCase, Serialization)
- Key design decisions implemented:
  * No UNIQUE(phone) at DB level (allows offline duplicates before sync resolves) — enforced at repository layer instead.
  * FK RESTRICT on deliveries→customers prevents silent loss of delivery history.
  * SyncOperation payload is pre-serialized JSON at insert time (durable across app restarts).
  * COMPLETE_DELIVERY auto-generates an idempotencyKey.
  * State machine is enforced both in domain (DeliveryStatus.canTransitionTo) and repository (DeliveryRepositoryImpl.transition).
- Next: Phase 4 (GPS & Location) — build LocationProvider interface + FusedLocationProvider implementation + permission handling + accuracy threshold logic + GetCurrentLocationUseCase.

---
Task ID: phase-4
Agent: main
Task: Phase 4 — GPS & Location: Build the location subsystem with LocationProvider abstraction, FusedLocationProvider implementation, permission handling, accuracy threshold logic, CaptureLocationScreen, and unit tests.

Work Log:
- Created core/location/ package.
- Wrote LocationException.kt — sealed class hierarchy: LocationPermissionException (with permanentlyDenied flag), GpsDisabledException (with isResolvable), LocationTimeoutException (with timeoutMillis), LocationUnavailableException (fallback).
- Wrote LocationProvider.kt — interface with getCurrentLocation() (single fix with timeout/threshold/maxAge), observeLocationUpdates() (Flow for navigation), hasLocationPermission(), isLocationEnabled(). Constants: DEFAULT_TIMEOUT_MILLIS=10s, DEFAULT_ACCURACY_THRESHOLD=10m, DEFAULT_MAX_AGE_MILLIS=30s.
- Wrote FusedLocationProvider.kt — production implementation using Play Services FusedLocationProviderClient. Strategy: try last known location first (cheap) → request fresh high-accuracy fix if stale/poor → use withTimeoutOrNull for deadline. Uses LocationRequest.Builder with PRIORITY_HIGH_ACCURACY, setWaitForAccurateLocation(true). Converts android.location.Location to domain LocationResult.
- Wrote LocationModule.kt — Hilt module providing FusedLocationProvider (singleton) + GpsSettingsHelper. GpsSettingsHelper has openLocationSettings() (ACTION_LOCATION_SOURCE_SETTINGS) and openAppDetailsSettings() (ACTION_APPLICATION_DETAILS_SETTINGS for permanently denied case).
- Wrote domain/usecase/location/GetCurrentLocationUseCase.kt — thin wrapper that delegates to LocationProvider with same parameters.
- Updated di/UseCaseModule.kt to provide GetCurrentLocationUseCase.
- Wrote core/ui/location/PermissionState.kt — enum (UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED) + rememberPermissionState() composable using ActivityResultContracts.RequestMultiplePermissions. Heuristic: denialCount >= 2 → PERMANENTLY_DENIED (system won't show dialog anymore).
- Wrote core/ui/components/ConfirmationDialog.kt — generic AlertDialog wrapper with title/message/confirm/dismiss.
- Wrote core/ui/components/AccuracyIndicator.kt — visual indicator with 3 tiers (EXCELLENT ≤5m, GOOD ≤threshold, POOR >threshold). Pure computeAccuracyTier() function extracted for testability. Green check icon for acceptable, orange warning for poor.
- Wrote presentation/location/LocationPermissionGate.kt — composable that wraps a screen requiring location permission. Shows rationale dialog first, then system permission dialog, then falls back to "open Settings" dialog for permanently denied.
- Wrote presentation/location/CaptureLocationViewModel.kt — HiltViewModel with sealed CaptureLocationUiState (Idle, Loading, Success, PoorAccuracy, Error). captureLocation() runs the use case, checks accuracy against threshold, maps exceptions to LocationErrorType enum.
- Wrote presentation/location/CaptureLocationScreen.kt — full screen UI with map placeholder, location coordinates display, AccuracyIndicator, capture button, retry button, error messages. Uses StateFlow from ViewModel via collectAsStateWithLifecycle.
- Updated presentation/navigation/Routes.kt — added CAPTURE_LOCATION route.
- Updated presentation/navigation/WaselNavHost.kt — added composable for CaptureLocationScreen wrapped in LocationPermissionGate.
- Added 23 new string resources in values/strings.xml (English) and values-ar/strings.xml (Arabic) for: location rationale, permission denied, GPS disabled, timeout, capture flow, accuracy tiers (excellent/good/poor).
- Wrote 4 test files (35 test methods):
  * FakeLocationProvider.kt — test double with permissionGranted, locationEnabled, locationToReturn, exceptionToThrow, updatesToEmit knobs.
  * GetCurrentLocationUseCaseTest.kt — 9 tests: returns location, default params propagation, custom params propagation, exception pass-through (permission/gps/timeout/runtime), call count.
  * CaptureLocationViewModelTest.kt — 14 tests using Turbine: Idle initial state, Success on acceptable accuracy, PoorAccuracy on poor accuracy, threshold boundary, exception mapping (permission/gps/timeout/unknown), reset(), multiple sequential captures.
  * AccuracyIndicatorTest.kt — 9 tests: tier boundaries (0/5/5.1/10/10.1/70), threshold adjustment effects, EXCELLENT independence from threshold.

Stage Summary:
- Phase 4 (GPS & Location) complete.
- 12 new Kotlin main files + 4 new test files added on top of Phase 3.
- Total project: 63 Kotlin main files + 8 test files = 71 Kotlin files.
- Location subsystem architecture:
  * core/location/ — LocationProvider interface + FusedLocationProvider impl + LocationException hierarchy + GpsSettingsHelper + Hilt LocationModule
  * core/ui/location/ — PermissionState enum + rememberPermissionState composable
  * core/ui/components/ — AccuracyIndicator (3-tier) + ConfirmationDialog
  * presentation/location/ — LocationPermissionGate, CaptureLocationScreen, CaptureLocationViewModel
  * domain/usecase/location/ — GetCurrentLocationUseCase
- Key design decisions:
  * Last-known-location-first strategy: try cheap instant fix before requesting fresh one (saves battery + time).
  * setWaitForAccurateLocation(true) on LocationRequest.Builder — Play Services will internally wait for a high-accuracy fix instead of returning the first noisy one.
  * Accuracy threshold is parameter-driven (default 10m, configurable in Settings later) — use case and UI both honor the same value.
  * Exception types map cleanly to UI states: each LocationException subclass has a corresponding LocationErrorType enum value.
  * Permission state has 4 distinct states (UNKNOWN, GRANTED, DENIED, PERMANENTLY_DENIED) — UI shows the right dialog for each.
  * FakeLocationProvider exposes knobs for every behavior the tests need to drive (no Robolectric required for use case/VM tests).
- Next: Phase 5 (Maps) — Mapbox integration: MapProvider interface, MapboxMapProvider impl, HomeScreen with full-screen map, driver marker, customer markers from Room, marker interaction.

---
Task ID: phase-5
Agent: main
Task: Phase 5 — Maps (Mapbox): Integrate Mapbox, build MapProvider abstraction, render driver + customer markers on the Home screen with marker interaction (bottom sheet).

Work Log:
- Created core/maps/ package with model/, mapbox/ subpackages.
- Wrote core/maps/model/MapModels.kt:
  * MarkerType enum (DRIVER, CUSTOMER, ACTIVE, DELIVERED, CANCELLED)
  * MapMarker data class (id, position, type, data)
  * CameraTarget sealed interface (Center / Fit)
  * MapTapResult sealed interface (OnMarker / OnPoint)
- Wrote core/maps/MapProvider.kt — interface with attach(), detach(), setMarkers(), moveCamera(), observeTaps() Flow, isReady() Flow. Lifecycle contract: attach() once per host, detach() releases resources.
- Wrote core/maps/mapbox/MapboxMapProvider.kt — production implementation:
  * Uses MapView (owned by Composable via AndroidView), bind(mapView) method
  * Loads Style.MAPBOX_STREETS, sets _isReady=true on style load
  * Uses PointAnnotationManager for markers (single manager, diffable updates)
  * OnMapClickListener with hitTest() — finds closest marker within 50m radius
  * Marker colors via colorHex() extension matching Color.kt constants
  * Camera flyTo for Center, cameraForCoordinates + flyTo for Fit
- Wrote core/maps/MapsModule.kt — Hilt module binding MapboxMapProvider as singleton MapProvider.
- Wrote domain/usecase/delivery/ObserveActiveDeliveryCustomerIdsUseCase.kt — streams Set<String> of customer IDs with active deliveries (ON_THE_WAY). Used to color those markers as ACTIVE.
- Updated di/UseCaseModule.kt to provide the new use case.
- Wrote presentation/home/HomeViewModel.kt:
  * HiltViewModel injecting ObserveCustomersUseCase, ObserveActiveDeliveryCustomerIdsUseCase, LocationProvider
  * Combines 4 flows: customers, activeCustomerIds, driverLocation, selectedCustomerId → single HomeUiState
  * startObservingDriverLocation() — low-frequency 15s updates when permission granted
  * onMarkerClicked(id) / onDismissSelection() for bottom sheet state
  * HomeUiState.markers() pure function deriving MapMarker list (DRIVER first, then customers colored by active state)
- Rewrote presentation/home/HomeScreen.kt:
  * Acquires MapProvider via Hilt EntryPoint (composables can't @Inject)
  * MapProviderEntryPoint interface for the entry point
  * AndroidView factory creates MapView, calls MapboxMapProvider.bind()
  * LaunchedEffect collects isReady() and observeTaps()
  * LaunchedEffect on state changes → setMarkers() + moveCamera() to driver
  * LocationPermissionGate wraps the map (still shows map if denied, just no driver marker)
  * Empty state hint when customers list is empty
  * "My location" FAB (bottom-end) + "Add customer" FAB (bottom-start)
  * ModalBottomSheet for selected customer: name, phone, coordinates, Start/Call/Details buttons
  * Call button uses Intent.ACTION_DIAL with tel: URI
  * DisposableEffect.onDispose calls mapProvider.detach()
- Added 7 new string resources in values/ and values-ar/ for: map_driver_location, map_customer_marker, map_active_marker, map_no_customers_hint, customer_call, customer_start_delivery, customer_view_details.
- Wrote 3 test files (32 test methods):
  * FakeMapProvider.kt — test double capturing lastMarkers, lastCameraTarget, call counts; supports emitTap() and setReady().
  * MapProviderContractTest.kt — 11 tests: attach/detach counts, setMarkers capture and replace, moveCamera with Center/Fit targets, isReady flow, observeTaps emits OnMarker/OnPoint, initial state.
  * HomeUiStateMarkersTest.kt — 11 tests: empty state, DRIVER marker from driverLocation, CUSTOMER vs ACTIVE based on activeCustomerIds, driver marker first, marker positions, multi-customer order, ghost-active-id ignored, data payload correctness.

Stage Summary:
- Phase 5 (Maps) complete.
- 6 new Kotlin main files + 3 new test files added on top of Phase 4.
- Total project: 69 Kotlin main files + 11 test files = 80 Kotlin files.
- Map architecture:
  * core/maps/model/ — MarkerType, MapMarker, CameraTarget, MapTapResult
  * core/maps/MapProvider — interface (SDK-agnostic)
  * core/maps/mapbox/MapboxMapProvider — production impl using Mapbox SDK
  * core/maps/MapsModule — Hilt binding
  * presentation/home/HomeViewModel — combines customer + delivery + location flows → markers
  * presentation/home/HomeScreen — full-screen map + FABs + bottom sheet
- Key design decisions:
  * MapView is owned by the Composable (via AndroidView), not by the provider. Provider binds to it via bind(mapView). This keeps Compose lifecycle in control.
  * Marker IDs are stable ("driver", "customer-{id}"). Mapbox SDK diffs by these.
  * Tap hit-testing uses 50m Haversine radius — forgiving for fingers on small markers.
  * Driver marker rendered first so customer markers paint on top.
  * activeCustomerIds drives marker color (CUSTOMER → ACTIVE) without touching domain models.
  * Composables acquire MapProvider via Hilt EntryPoint (composables can't @Inject).
  * Low-frequency driver updates (15s) on home screen for battery. Higher frequency (3s) reserved for navigation in Phase 15.
- Next: Phase 6 (Customer Management) — full CRUD: AddCustomerViewModel wired to AddCustomerUseCase, real customer list with search, edit/delete customer screens, customer details screen, duplicate phone detection, location capture integrated into add flow.

---
Task ID: phase-6
Agent: main
Task: Phase 6 — Customer Management: Wire ViewModels to real UseCases for full CRUD (list with search, add with GPS capture + duplicate detection, details with delete confirmation, edit with location update).

Work Log:
- Wrote presentation/customers/CustomerListViewModel.kt:
  * HiltViewModel injecting ObserveCustomersUseCase + SearchCustomersUseCase
  * Uses flatMapLatest on _query flow: blank → observeAll, non-blank → search
  * CustomerListUiState with computed isEmpty / isSearching / isNoResults
- Rewrote presentation/customers/CustomerListScreen.kt:
  * Real OutlinedTextField bound to viewModel::onQueryChange
  * Clear button (X icon) when query non-empty
  * LazyColumn with CustomerRow (avatar circle with first letter + name + phone + coordinates)
  * Empty state when no customers, no-results state when search returns empty
  * FAB to add customer
- Wrote presentation/customers/AddCustomerViewModel.kt:
  * HiltViewModel injecting AddCustomerUseCase + GetCurrentLocationUseCase + CheckDuplicatePhoneUseCase
  * State machine: Idle → CapturingLocation → Idle (with location) → Saving → Saved
  * canSave computed property: name 2-120, phone 7-30, location != null, not saving/capturing
  * Duplicate phone check before save (returns existing customer's name in error)
  * validateName() + validatePhone() + Throwable.toUserMessage() extracted as internal helpers for testability
- Rewrote presentation/customers/AddCustomerScreen.kt:
  * OutlinedTextField for name + phone with supportingText errors
  * LocationPermissionGate wrapping the location section
  * Location section: capture button / captured display + AccuracyIndicator / re-capture button
  * SnackbarHost for locationError + errorMessage
  * LaunchedEffect on isSaved → onSaved() callback
- Wrote presentation/customers/CustomerDetailsViewModel.kt:
  * HiltViewModel injecting GetCustomerUseCase + DeleteCustomerUseCase
  * _customerId flow + flatMapLatest on customer flow
  * AuxState (isDeleting, isDeleted, deleteError, showDeleteConfirm) combined with customer flow
  * delete() runs DeleteCustomerUseCase, sets isDeleted on success or deleteError on failure
- Wrote presentation/customers/CustomerDetailsScreen.kt:
  * TopAppBar with back + edit action
  * Customer info: name (headline), phone (with dial intent), coordinates, GPS accuracy (AccuracyIndicator), last updated
  * Primary: Start Delivery button (Phase 13 wires)
  * Secondary: Call button (Intent.ACTION_DIAL)
  * Tertiary row: Edit + Delete buttons
  * ConfirmationDialog for delete with customer name in message
  * Loading state (CircularProgressIndicator) while customer loads
  * SnackbarHost for deleteError
- Wrote presentation/customers/EditCustomerViewModel.kt:
  * HiltViewModel injecting GetCustomerUseCase + UpdateCustomerUseCase + UpdateCustomerLocationUseCase + GetCurrentLocationUseCase
  * FormState tracks name/phone/location/error flags separately from originalCustomer
  * First load pre-fills form from customer flow; subsequent edits preserve user input
  * hasUnsavedChanges computed property
  * save() calls UpdateCustomerUseCase (if name/phone changed) then UpdateCustomerLocationUseCase (if location re-captured)
- Wrote presentation/customers/EditCustomerScreen.kt:
  * Same form layout as Add (name + phone + location)
  * Location section shows existing customer location with "Update location" button
  * After re-capture: shows new location with AccuracyIndicator + "Re-capture" button
  * Loading state while customer loads
- Updated presentation/navigation/WaselNavHost.kt:
  * Added composable for Routes.CUSTOMER_DETAILS with customerId NavType.StringType argument
  * Added composable for Routes.EDIT_CUSTOMER with customerId argument
  * Wired onEdit and onDeleted callbacks
- Added 13 new string resources in values/ and values-ar/ for: customer details title, GPS accuracy, last updated, edit/delete actions, delete confirm title + message, customer loading, customer not found, edit customer title, update location.
- Wrote 3 test files (40 test methods):
  * ValidationHelpersTest.kt — 21 tests covering validateName (4 cases), validatePhone (5 cases), Throwable.toUserMessage (6 cases for each LocationException subclass), AddCustomerUiState.canSave (6 cases)
  * CustomerListUiStateTest.kt — 9 tests covering isEmpty, isSearching, isNoResults, total
  * AddCustomerViewModelTest.kt — 10 tests using FakeCustomerRepository + FakeLocationProvider:
    - Initial state
    - onNameChange/onPhoneChange
    - captureLocation success + timeout
    - canSave becomes true when valid
    - save calls repository and sets isSaved
    - save with duplicate phone sets phoneError
    - save with short name/phone sets validation errors
    - resetSaved clears flag
  * FakeCustomerRepository — minimal fake tracking addedCustomers + existingByPhone for duplicate testing.

Stage Summary:
- Phase 6 (Customer Management) complete.
- 8 new Kotlin main files + 3 new test files added on top of Phase 5.
- Total project: 75 Kotlin main files + 14 test files = 89 Kotlin files.
- Customer CRUD fully wired:
  * List with reactive search (flatMapLatest on query flow)
  * Add with GPS capture + duplicate phone detection + form validation
  * Details with delete confirmation + Call action
  * Edit with name/phone update + location re-capture
- Navigation graph now has 9 routes: splash, login, home, customers, history, settings, add_customer, customer_details, edit_customer, capture_location.
- Validation helpers (validateName, validatePhone, toUserMessage) extracted as internal functions for direct unit testing without ViewModel instantiation.
- Key UX decisions:
  * Avatar circle uses first letter of name — gives visual variety in the list without icon assets
  * Delete uses a confirmation dialog (not a swipe action) because customer deletion is destructive and blocked by active deliveries
  * Edit screen pre-fills form from customer flow and re-emits when Room updates (form stays in sync if customer changes from another flow)
  * Location re-capture is a separate button in Edit (not automatic) because we want explicit user intent for GPS updates
- Next: Phase 7 (Backend Foundation) — FastAPI project setup, PostgreSQL schema migrations, docker-compose, Alembic, seed script, /health endpoint.

---
Task ID: phase-7
Agent: main
Task: Phase 7 — Backend Foundation: Build the FastAPI backend skeleton with config, async DB, security, unified errors, SQLAlchemy models, Alembic migration, Docker, seed script, and initial tests.

Work Log:
- Created backend/ directory tree: app/{api,core,models,schemas,services,repositories,middleware,utils}, migrations/versions, scripts, tests.
- Wrote requirements.txt — FastAPI 0.115 + uvicorn + SQLAlchemy 2.0 async + asyncpg + alembic + Pydantic v2 + pydantic-settings + python-jose + passlib[argon2] + httpx + slowapi + structlog.
- Wrote requirements-dev.txt — pytest + pytest-asyncio + ruff + black + mypy + ipython.
- Wrote .env.example with all env vars documented (APP, DATABASE, JWT, CORS, RATE_LIMIT, MAPBOX, FCM, LOG).
- Wrote app/core/config.py — Pydantic Settings with case_sensitive=False, env_file=".env". Validators: JWT_SECRET warns if still default. Properties: cors_origins_list, is_production, is_development. Singleton via @lru_cache get_settings().
- Wrote app/core/database.py — async engine (asyncpg, pool_pre_ping=True, pool_size=10, max_overflow=20), AsyncSessionLocal factory, get_db() FastAPI dependency (yields session, commits on success, rolls back on error), check_db_connection() for /health probe. Base declarative class for all models.
- Wrote app/core/security.py:
  * Argon2id password hashing via passlib.CryptContext (memory_cost=64MB, time_cost=3, parallelism=4 — OWASP-recommended).
  * hash_password() + verify_password() (verify never raises — returns False on any failure).
  * create_access_token() (15min default) + create_refresh_token() (30d default, includes jti).
  * decode_token() raises typed errors: TokenExpiredError vs TokenInvalidError.
- Wrote app/core/exceptions.py:
  * AppError base class with status_code, code, message, details.
  * 9 subclasses: BadRequestError, UnauthorizedError, ForbiddenError, NotFoundError, ConflictError, ValidationError, RateLimitError, InternalError, ServiceUnavailableError.
  * ErrorCodes class with 20 string constants (INVALID_CREDENTIALS, TOKEN_EXPIRED, CUSTOMER_NOT_FOUND, DUPLICATE_PHONE, INVALID_STATE_TRANSITION, IDEMPOTENCY_CONFLICT, STALE_UPDATE, ...).
  * format_error_response() helper producing the unified {error: {code, message, details}} shape.
- Wrote app/core/logging.py — structlog JSON formatter for production, plain text for dev. Bridges stdlib logging → structlog.
- Wrote 6 SQLAlchemy models:
  * user.py — id (UUID PK), username (unique), password_hash, role (CHECK driver/admin), is_active, timestamps, last_login_at.
  * customer.py — id, driver_id (FK CASCADE), name, phone, latitude (NUMERIC 10,7), longitude, accuracy, timestamps. UNIQUE(driver_id, phone). CHECK on lat/lng range, name/phone length, accuracy non-negative.
  * delivery.py — id, customer_id (FK RESTRICT), driver_id (FK CASCADE), status, timestamps. CHECK on status enum + timestamp/state consistency (PENDING has all nulls, DELIVERED has started+completed, etc.).
  * refresh_token.py — id, user_id (FK CASCADE), token_hash (unique), expires_at, revoked, created_at, device_info.
  * audit_log.py — id, user_id (FK SET NULL), action, entity_type, entity_id, metadata (JSONB), ip_address, created_at.
  * idempotency_key.py — id, key (unique), user_id (FK CASCADE), endpoint, request_hash, response (JSONB), status_code, created_at, expires_at (default +24h).
  * models/__init__.py re-exports all for convenient imports.
- Wrote app/api/v1.py — empty router with /health inside /api/v1.
- Wrote app/main.py — create_app() factory with lifespan (configures logging), CORS middleware (origins from settings), exception handlers (AppError → unified response, SQLAlchemyError → 500, generic Exception → 500), root /health endpoint (DB-aware), include v1_router under /api/v1. Docs/redoc/openapi disabled in production.
- Wrote migrations/env.py — async Alembic env, reads URL from settings, target_metadata = Base.metadata, compare_type=True.
- Wrote migrations/script.py.mako — Alembic template.
- Wrote migrations/versions/0001_initial_schema.py — single migration creating all 6 tables with their indexes, FKs, UNIQUE constraints, and CHECK constraints matching the ORM models.
- Wrote alembic.ini — standard config pointing to migrations/ with placeholder URL (env.py overrides it).
- Wrote Dockerfile — multi-stage build: builder stage installs deps with build-essential, runtime stage uses python:3.11-slim with libpq5 only. Non-root user (waselni, uid 1000). HEALTHCHECK hits /health.
- Wrote docker-compose.yml — PostgreSQL 15-alpine with healthcheck + FastAPI backend with hot-reload via --reload and volume mount. Backend depends on db healthcheck.
- Wrote scripts/seed.py — creates admin (admin/admin12345) + driver_01 (driver/driver12345) if not present. --reset flag truncates all tables (DEV ONLY, requires confirmation).
- Wrote tests/conftest.py:
  * TEST_DATABASE_URL = settings.DATABASE_URL.replace("/waslni", "/waslni_test").
  * test_engine + TestSessionLocal bound to test DB.
  * setup_database fixture (session-scoped, autouse) — creates all tables once.
  * truncate_tables fixture (function-scoped, autouse) — TRUNCATE all tables between tests.
  * db_session, app_with_db (overrides get_db), client (httpx ASGITransport), test_user, auth_headers fixtures.
- Wrote pytest.ini — async mode auto, strict markers, testpaths=tests.
- Wrote 4 test files (28 test methods):
  * test_config.py (6 tests) — defaults, CORS parsing, is_production/is_development, JWT_SECRET handling.
  * test_exceptions.py (12 tests) — hierarchy, status codes, code/message/details, format_error_response minimal + with details + None handling, all ErrorCodes uppercase alphanumeric.
  * test_security.py (10 tests) — Argon2 hashing (starts with $argon2, different salt each time, verify accepts/rejects, returns False on garbage), JWT access token create+decode, refresh token with jti, tampered token rejected, expired token raises TokenExpiredError.
  * test_health.py (3 tests) — /api/v1/health returns ok, /health returns 200, /openapi.json available in dev.
- Wrote README.md with Quick Start (Docker + Local), Testing instructions, default credentials, project structure.

Stage Summary:
- Phase 7 (Backend Foundation) complete.
- 31 Python files + 6 config/Docker files + README produced.
- Backend is fully runnable: `docker compose up -d && alembic upgrade head && python -m scripts.seed` produces a working FastAPI server with /health, /docs, /redoc.
- Test suite (28 tests) covers config, exceptions, security primitives, and the /health endpoint.
- Architecture:
  * app/core/ — config, database, security, exceptions, logging (5 modules)
  * app/models/ — 6 SQLAlchemy ORM models
  * app/api/v1.py — empty router (Phase 8 adds /auth)
  * app/main.py — app factory with CORS + exception handlers + /health
  * migrations/ — async Alembic with single initial migration
  * tests/ — pytest-asyncio + httpx + isolated test DB
- Key design decisions:
  * All timestamps use timezone=True (UTC storage).
  * UUID PKs generated client-side (default=uuid4 in Python, server_default=gen_random_uuid() in DB).
  * Composite UNIQUE(driver_id, phone) on customers — different drivers can share a phone number.
  * FK on deliveries.customer_id uses RESTRICT (never silently lose delivery history).
  * CHECK constraints enforce state-machine consistency at the DB level (defense in depth).
  * Argon2id with OWASP-recommended parameters (64MB / time 3 / parallel 4).
  * Refresh tokens stored as SHA-256 hashes (never plaintext) — enables revocation.
  * Idempotency keys expire after 24h via server_default.
  * Exception handlers convert everything (including unhandled exceptions) to the unified error response — never leak internals.
- Next: Phase 8 (Backend Auth) — /auth router (login, refresh, logout, me), AuthService, get_current_user dependency, audit logging, rate limiting on /auth/login.

---
Task ID: phase-8
Agent: main
Task: Phase 8 — Backend Auth: Build /auth router (login, refresh, logout, me) with AuthService, repositories, get_current_user dependency, audit logging, rate limiting, and comprehensive tests.

Work Log:
- Wrote app/schemas/auth.py:
  * LoginRequest (username min 3 max 50, password min 1 max 128, username trimmed)
  * RefreshRequest, LogoutRequest
  * UserResponse (from_attributes=True, never includes password_hash)
  * TokenResponse (access_token, refresh_token, token_type="bearer", expires_in, user)
- Wrote app/schemas/common.py — PaginatedResponse[T], PaginationMeta, ErrorResponse, ErrorBody for reuse across future endpoints.
- Wrote app/repositories/user_repo.py — get_by_id, get_by_username, update_last_login.
- Wrote app/repositories/refresh_token_repo.py:
  * _hash_token() — SHA-256 of refresh token (never stored plaintext).
  * create() — stores hash + expiry + device_info.
  * get_by_token() — lookup by hash.
  * revoke() — single token, revoke_all_for_user() — for logout-all + reuse detection.
  * delete_expired() — periodic cleanup.
  * default_expiry() — settings.REFRESH_TOKEN_EXPIRE_DAYS from now.
- Wrote app/repositories/audit_log_repo.py — record() inserts AuditLog with action, user_id, entity info, metadata (JSONB), ip_address. Doc clearly states metadata must NEVER contain passwords/tokens/phones.
- Updated app/core/exceptions.py — added InvalidCredentialsError (401) and AccountDisabledError (403).
- Wrote app/services/auth_service.py:
  * login(username, password, ip) — always runs password verify even if user is None (timing-safe against username enumeration). Raises InvalidCredentialsError for unknown user OR wrong password (same error). Raises AccountDisabledError for inactive users. Issues access + refresh token, persists refresh hash, updates last_login_at, records LOGIN_SUCCESS audit log.
  * refresh(refresh_token, ip) — decodes JWT, looks up stored hash, checks revoked flag. On revoked-token reuse: revokes ALL of the user's tokens (suspected theft) and records TOKEN_REVOKED audit log. Rotation strategy: old refresh token revoked, new pair issued. Records TOKEN_REFRESHED audit log.
  * logout(refresh_token, ip) — idempotent. If token exists and not revoked, revokes it and records LOGOUT audit log. If unknown or already revoked, returns silently (no information leak).
- Wrote app/api/deps.py:
  * DbSession = Annotated[AsyncSession, Depends(get_db)]
  * get_auth_service(session) — constructs AuthService with all 3 repositories bound to the session
  * AuthServiceDep = Annotated[AuthService, Depends(get_auth_service)]
  * get_current_user(session, authorization) — extracts Bearer token, decodes JWT, looks up User. Raises typed errors: UNAUTHORIZED (no header), TOKEN_INVALID (bad signature/wrong type), TOKEN_EXPIRED. Returns User row.
  * CurrentUser = Annotated[User, Depends(get_current_user)]
  * get_current_active_user(user) — additionally requires is_active=True. Raises ACCOUNT_DISABLED.
  * ActiveUser = Annotated[User, Depends(get_current_active_user)]
  * get_client_ip(request) — honors X-Forwarded-For for proxies.
- Wrote app/api/auth.py:
  * POST /auth/login — calls AuthService.login, rate-limited via _check_login_rate_limit (5/min/IP via X-Forwarded-For, sliding window in-memory).
  * POST /auth/refresh — calls AuthService.refresh.
  * POST /auth/logout — calls AuthService.logout, returns 204 (idempotent).
  * GET /auth/me — ActiveUser dependency, returns UserResponse.
- Updated app/api/v1.py — include auth_router.
- Wrote tests/test_auth.py (18 test methods in 5 test classes):
  * TestLogin (6 tests) — success, wrong password, unknown user (same error as wrong password), disabled account, validation error, audit log creation.
  * TestRefresh (4 tests) — success, rotation (old token revoked), tampered token, access token rejected by refresh endpoint.
  * TestLogout (3 tests) — success (token can no longer refresh), unknown token idempotent, double logout idempotent.
  * TestMe (5 tests) — success, no token → 401, garbage token → 401, refresh token rejected → 401, disabled user → 403.
  * TestRateLimit (1 test) — 6th attempt from same IP returns 429 RATE_LIMIT_EXCEEDED.

Stage Summary:
- Phase 8 (Backend Auth) complete.
- 11 new Python files added on top of Phase 7's 31.
- Total backend: 40 Python files.
- Auth system fully functional:
  * Login with username + password → access (15min) + refresh (30d) tokens
  * Refresh with rotation (old token revoked, new pair issued)
  * Reuse detection (revoked token use → revoke ALL user tokens)
  * Logout idempotent (safe to retry)
  * /auth/me with active user check
  * Rate limiting (5/min/IP via X-Forwarded-For)
  * Audit logging for LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, TOKEN_REFRESHED, TOKEN_REVOKED
- Key security decisions:
  * Same error code for "user not found" and "wrong password" — no username enumeration via timing or distinct codes.
  * Always run verify_password (even for None user) to equalize timing.
  * Refresh tokens stored as SHA-256 hashes — DB leak doesn't expose valid tokens.
  * Refresh rotation: old token revoked on every refresh, limits blast radius of stolen tokens.
  * Revoked-token reuse triggers logout-all (defense against token theft).
  * Logout is idempotent — no information about whether token existed.
  * Refresh tokens rejected by /auth/me (and access tokens rejected by /auth/refresh) — strict type checking.
  * ActiveUser dependency (separate from CurrentUser) for endpoints that perform actions.
- Next: Phase 9 (Backend Customers & Deliveries) — CRUD endpoints with authorization (driver A cannot access driver B's data), pagination, search, delivery state machine validation.
