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

---
Task ID: phase-9
Agent: main
Task: Phase 9 — Backend Customers & Deliveries: Build CRUD endpoints with authorization, pagination, search, delivery state machine, Idempotency-Key support, and comprehensive tests.

Work Log:
- Wrote app/schemas/customer.py:
  * CustomerCreate (name 2-120, phone 7-30, lat -90..90, lng -180..180, accuracy ≥0 nullable). Validators: name.strip(), phone normalized (strip spaces/dashes/parens).
  * CustomerUpdate (all fields optional, same validators).
  * CustomerResponse (from_attributes, never includes password_hash).
- Wrote app/schemas/delivery.py:
  * DeliveryStatusEnum (PENDING, ASSIGNED, ON_THE_WAY, ARRIVED, DELIVERED, CANCELLED).
  * DeliveryCreate (customer_id UUID, status default ON_THE_WAY).
  * DeliveryStatusUpdate (status enum).
  * DeliveryResponse (id, customer_id, nested customer, status, all timestamps).
- Wrote app/repositories/customer_repo.py:
  * All queries scoped by driver_id for authorization.
  * get_by_id, get_by_phone (with exclude_id for update flow), list_customers (paginated + search via ILIKE prefix on name OR phone), create, update_fields (bumps updated_at server-side), delete (returns bool), has_active_delivery (checks ON_THE_WAY/ARRIVED).
- Wrote app/repositories/delivery_repo.py:
  * get_by_id with selectinload(Delivery.customer).
  * list_deliveries with filters: status, from_date, to_date.
  * get_active_for_customer (used to block duplicate active deliveries).
  * create (with refresh to populate customer relationship).
  * update_status (sets status + optional timestamp fields, returns updated row with customer loaded).
- Updated app/models/delivery.py — added customer: Mapped["Customer"] relationship(lazy="selectin") so responses can eager-load customer data.
- Wrote app/repositories/idempotency_repo.py:
  * _hash_request(payload) — stable SHA-256 of JSON body for conflict detection.
  * lookup(key, user_id) — fetch existing record.
  * store(key, user_id, endpoint, request_payload, response, status_code, ttl=24h) — persist.
  * compute_request_hash(payload) — exposed for service-layer comparison.
  * delete_expired() — periodic cleanup.
- Wrote app/services/customer_service.py:
  * create(driver_id, data, ip) — duplicate phone check, audit log CREATE_CUSTOMER.
  * get(customer_id, driver_id) — scoped fetch.
  * list(driver_id, page, limit, search) — paginated.
  * update(customer_id, driver_id, data, ip) — only non-None fields applied, duplicate phone check on phone change, audit log UPDATE_CUSTOMER.
  * delete(customer_id, driver_id, ip) — blocked by active delivery (raises CUSTOMER_HAS_ACTIVE_DELIVERY), audit log DELETE_CUSTOMER.
  * _require_customer() — same NotFoundError for "doesn't exist" and "belongs to another driver" (no info leak).
- Wrote app/services/delivery_service.py:
  * _TRANSITIONS dict — state machine: PENDING → ASSIGNED/CANCELLED, ASSIGNED → ON_THE_WAY/CANCELLED, ON_THE_WAY → ARRIVED/CANCELLED, ARRIVED → DELIVERED/CANCELLED, DELIVERED/CANCELLED → terminal.
  * _is_allowed_transition() — O(1) lookup.
  * create(driver_id, data, ip) — customer must exist + belong to driver, no existing active delivery, sets started_at if ON_THE_WAY, audit log START_DELIVERY.
  * get(delivery_id, driver_id) — scoped fetch.
  * list(driver_id, page, limit, status, from_date, to_date) — paginated + filtered.
  * transition(delivery_id, driver_id, target, idempotency_key, request_payload, ip):
    * Idempotency check first — if key exists with same hash → return cached response; if key exists with different hash → 409 IDEMPOTENCY_CONFLICT.
    * Validate transition via _is_allowed_transition() — 409 INVALID_STATE_TRANSITION if not allowed.
    * Apply timestamp based on target (arrived_at / completed_at / cancelled_at / started_at).
    * Audit log START_DELIVERY / ARRIVE_DELIVERY / COMPLETE_DELIVERY / CANCEL_DELIVERY.
    * Cache response under idempotency key if provided.
- Updated app/api/deps.py — added CustomerServiceDep + DeliveryServiceDep providers (construct services with all required repositories bound to the request session).
- Wrote app/api/customers.py — 5 endpoints:
  * GET /customers (paginated, search via ?search=)
  * POST /customers (201 on success, 409 on duplicate phone)
  * GET /customers/{id} (404 if not found OR belongs to another driver)
  * PATCH /customers/{id} (partial update, 409 on duplicate phone)
  * DELETE /customers/{id} (204 on success, 409 if active delivery exists)
  * All endpoints require ActiveUser.
- Wrote app/api/deliveries.py — 4 endpoints:
  * GET /deliveries (paginated, filters: status, from_date, to_date)
  * POST /deliveries (201 on success, 404 if customer unknown, 409 if active delivery exists)
  * GET /deliveries/{id} (404 if not found)
  * PATCH /deliveries/{id}/status (accepts Idempotency-Key header, 409 on invalid transition or idempotency conflict)
  * All endpoints require ActiveUser.
- Updated app/api/v1.py — include customers_router + deliveries_router.
- Wrote tests/test_customers.py (21 test methods in 5 classes):
  * TestCreateCustomer (6 tests) — success, phone normalization, duplicate phone, validation (short name, bad latitude), requires auth.
  * TestGetCustomer (3 tests) — success, unknown → 404, other driver's customer → 404 (no info leak).
  * TestListCustomers (5 tests) — empty, with customers (sorted), pagination, search by name, search by phone.
  * TestUpdateCustomer (4 tests) — update name, duplicate phone conflict, partial update only changes provided fields, unknown → 404.
  * TestDeleteCustomer (4 tests) — success, unknown → 404, blocked by active delivery, other driver's customer → 404.
- Wrote tests/test_deliveries.py (18 test methods in 6 classes):
  * TestCreateDelivery (4 tests) — success (with nested customer), unknown customer → 404, duplicate active delivery → 409, new delivery after previous delivered succeeds.
  * TestGetDelivery (2 tests) — success, unknown → 404.
  * TestListDeliveries (2 tests) — empty, filter by status.
  * TestStateMachine (5 tests) — valid ON_THE_WAY→ARRIVED, valid ARRIVED→DELIVERED, invalid skip ON_THE_WAY→DELIVERED → 409, transition from terminal (CANCELLED) → 409, cancel from ON_THE_WAY.
  * TestIdempotency (3 tests) — same key + same body returns cached response (same arrived_at), same key + different body → 409 IDEMPOTENCY_CONFLICT, no key allows re-execution (second call hits state machine).
  * TestAuthorization (2 tests) — other driver cannot GET delivery → 404, other driver cannot PATCH delivery → 404.

Stage Summary:
- Phase 9 (Backend Customers & Deliveries) complete.
- 12 new Python files added on top of Phase 8's 40.
- Total backend: 51 Python files.
- API surface complete for MVP:
  * /auth (4 endpoints) — Phase 8
  * /customers (5 endpoints) — Phase 9
  * /deliveries (4 endpoints) — Phase 9
  * /sync (1 endpoint) — Phase 10 (next)
- Authorization fully enforced:
  * All customer/delivery queries scoped by driver_id (extracted from JWT).
  * Same 404 error for "doesn't exist" and "belongs to another driver" — no info leak.
  * ActiveUser dependency (requires is_active=True) on all mutating endpoints.
- State machine enforced:
  * Backend _TRANSITIONS dict validates every transition.
  * DB CHECK constraint validates timestamp/status consistency (defense in depth).
  * Terminal states (DELIVERED, CANCELLED) reject all further transitions.
- Idempotency fully implemented:
  * PATCH /deliveries/{id}/status accepts Idempotency-Key header.
  * Same key + same body → cached response (no re-execution).
  * Same key + different body → 409 IDEMPOTENCY_CONFLICT.
  * No key → state machine applies (second call may 409 if already in target state).
- Audit logging for: CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, START_DELIVERY, ARRIVE_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY.
- 39 new test methods (21 customers + 18 deliveries) covering happy path, error cases, authorization, state machine, idempotency.
- Next: Phase 10 (Backend Sync) — /sync endpoint that accepts a batch of pending operations from Android, processes each one, returns per-op results + server_changes for download direction.

---
Task ID: phase-10
Agent: main
Task: Phase 10 — Backend Sync Endpoint: Build POST /sync that accepts a batch of pending operations from Android, processes each one independently, returns per-op results (SUCCESS/CONFLICT/FAILED/IGNORED), server_changes for download direction, and a new latest_sync_timestamp.

Work Log:
- Wrote app/schemas/sync.py:
  * SyncOperationType enum (CREATE_CUSTOMER, UPDATE_CUSTOMER, DELETE_CUSTOMER, CREATE_DELIVERY, UPDATE_DELIVERY, COMPLETE_DELIVERY, CANCEL_DELIVERY).
  * SyncEntityType enum (CUSTOMER, DELIVERY).
  * SyncOperationRequest (id, entity_type, operation, payload dict, optional idempotency_key).
  * SyncRequest (operations list max 500, optional latest_sync_timestamp).
  * SyncResultStatus enum (SUCCESS, CONFLICT, FAILED, IGNORED).
  * SyncOperationResult (operation_id, status, entity_id, server_state for SUCCESS/CONFLICT, error dict).
  * ServerChange (entity_type, entity_id, operation, payload — for download direction).
  * SyncResponse (results, server_changes, latest_sync_timestamp).
- Wrote app/services/sync_service.py:
  * SyncService class with process(driver_id, request, ip) → SyncResponse.
  * _process_one() — wraps each operation in try/except, maps exceptions to result statuses (ConflictError → CONFLICT with server_state, NotFoundError → IGNORED for DELETE/UPDATE or FAILED for CREATE, AppError → FAILED, generic Exception → FAILED with INTERNAL_ERROR).
  * _dispatch() — routes to _process_customer_op() or _process_delivery_op() based on entity_type.
  * _process_customer_op():
    - CREATE_CUSTOMER → CustomerService.create()
    - UPDATE_CUSTOMER → conflict check (latest-write-wins): if server.updated_at > payload.updated_at → CONFLICT with server_state. Otherwise CustomerService.update().
    - DELETE_CUSTOMER → CustomerService.delete()
  * _process_delivery_op():
    - CREATE_DELIVERY → DeliveryService.create()
    - COMPLETE_DELIVERY → DeliveryService.transition(target=DELIVERED, idempotency_key=op.idempotency_key)
    - CANCEL_DELIVERY → DeliveryService.transition(target=CANCELLED, idempotency_key=op.idempotency_key)
    - UPDATE_DELIVERY → DeliveryService.transition(target=payload.status, idempotency_key=op.idempotency_key)
  * _compute_server_changes(driver_id, since):
    - If since is None (first sync) → return [] (client already has its own data).
    - Query customers WHERE driver_id AND updated_at > since → emit CREATE_CUSTOMER or UPDATE_CUSTOMER based on whether created_at ≈ updated_at.
    - Query deliveries for the driver → compute modified_at as max of all timestamps → if > since, emit appropriate operation based on status.
  * _lookup_server_state() — for CONFLICT results, fetches the current server version so the client can adopt it.
  * _TIMESTAMP_GRACE = 5 seconds — the new latest_sync_timestamp is "now - 5s" so concurrent writes aren't missed on next sync.
  * Audit log SYNC_PERFORMED with operations_count/success_count/conflict_count/failed_count metadata.
- Updated app/api/deps.py — added SyncServiceDep provider with customer_service_factory + delivery_service_factory closures that bind per-operation service instances to the same session (atomic batch commit).
- Wrote app/api/sync.py — POST /sync endpoint. Accepts SyncRequest, requires ActiveUser, returns SyncResponse. The batch is processed atomically — all operations commit or none do, but per-op failures are still reported individually.
- Updated app/api/v1.py — include sync_router.
- Wrote tests/test_sync.py (17 test methods in 7 test classes):
  * TestSyncCreateCustomer (2 tests) — create via sync success, duplicate phone returns CONFLICT.
  * TestSyncUpdateCustomer (2 tests) — update via sync success, stale update (server has newer) returns CONFLICT with server_state.
  * TestSyncDeleteCustomer (2 tests) — delete via sync success (verified gone), delete already-deleted returns IGNORED.
  * TestSyncCreateDelivery (1 test) — create delivery via sync with nested customer response.
  * TestSyncCompleteDelivery (2 tests) — complete with idempotency_key, idempotent retry returns cached response (same completed_at).
  * TestBatchProcessing (3 tests) — multiple ops in one batch, failure in one op doesn't block others, empty batch returns empty results + timestamp.
  * TestServerChanges (3 tests) — first sync returns no changes, changes since last sync are returned, latest_sync_timestamp advances.
  * TestSyncAuthorization (1 test) — driver B cannot UPDATE driver A's customer via sync.

Stage Summary:
- Phase 10 (Backend Sync) complete.
- 4 new Python files added on top of Phase 9's 51.
- Total backend: 55 Python files.
- Complete API surface for MVP:
  * /auth (4 endpoints) — Phase 8
  * /customers (5 endpoints) — Phase 9
  * /deliveries (4 endpoints) — Phase 9
  * /sync (1 endpoint) — Phase 10
  * /health (root + v1) — Phase 7
- Sync architecture:
  * Batch processing — one HTTP call carries up to 500 operations.
  * Independent processing — per-op failures don't abort the batch.
  * Atomic commit — all operations share the same session/transaction.
  * Conflict resolution:
    - Customers: latest-write-wins based on updated_at. Server returns CONFLICT + server_state so client adopts the server version.
    - Delivery completion: idempotency_key prevents double-execution on retry.
  * Download direction: server_changes returns entities modified since the client's last sync — enables multi-device sync.
  * Timestamp grace period: 5-second buffer so concurrent writes aren't missed.
- Result status mapping:
  * SUCCESS — operation applied, server_state returned.
  * CONFLICT — server has newer version (customers) or duplicate detected; server_state returned for adoption.
  * IGNORED — operation no longer applicable (e.g. DELETE for already-deleted entity).
  * FAILED — validation error, unknown entity, or internal error.
- 17 new test methods covering: CREATE/UPDATE/DELETE for customers, CREATE/COMPLETE for deliveries, batch processing, conflict scenarios, idempotency, multi-device sync, authorization.
- Backend MVP is now feature-complete. Next phases (11+) integrate Android with the backend:
  * Phase 11 — Android ↔ Backend Integration (Retrofit, auth interceptor, token authenticator, real login flow)
  * Phase 12 — Offline Sync (SyncWorker via WorkManager, retry logic, conflict handling on Android side)

---
Task ID: phase-11
Agent: main
Task: Phase 11 — Android ↔ Backend Integration: Wire the Android app to the real FastAPI backend with Retrofit + OkHttp + secure token storage + auto-refresh + unified error mapping + real login/splash flow.

Work Log:
- Created 6 DTO files (data/remote/dto/):
  * CommonDto.kt — ErrorDto, ErrorBody, PaginatedResponse<T>, PaginationMeta
  * AuthDto.kt — LoginRequestDto, RefreshRequestDto, LogoutRequestDto, TokenResponseDto, UserDto
  * CustomerDto.kt — CustomerCreateDto, CustomerUpdateDto, CustomerResponseDto
  * DeliveryDto.kt — DeliveryCreateDto, DeliveryStatusUpdateDto, DeliveryResponseDto
  * SyncDto.kt — SyncRequestDto, SyncOperationDto, SyncResponseDto, SyncOperationResultDto, ServerChangeDto
- Created 4 API interfaces (data/remote/api/):
  * AuthApi — login, refresh, logout, getMe
  * CustomerApi — listCustomers (paginated + search), createCustomer, getCustomer, updateCustomer, deleteCustomer
  * DeliveryApi — listDeliveries (paginated + filters), createDelivery, getDelivery, updateStatus (with Idempotency-Key header)
  * SyncApi — sync (with optional Idempotency-Key)
- Created core/network/ApiException.kt — sealed class hierarchy with 12 subtypes:
  * NoConnection, Timeout (network-level)
  * Unauthorized, Forbidden, NotFound, Conflict, Validation, RateLimited (HTTP)
  * ServerError, HttpError (5xx + other)
  * ParseError, Unknown
  Each carries an Arabic user-facing message.
- Created core/security/SecureStorage.kt — EncryptedSharedPreferences wrapper (AES-256-GCM, Keystore-backed master key). Properties: accessToken, refreshToken, userId, username, role. clearAuth() wipes all auth fields.
- Created core/security/TokenManager.kt — high-level token manager with refreshMutex() for serializing concurrent refresh attempts. Methods: hasSession(), saveTokens(), updateAccessToken(), clearSession(), getCachedUser().
- Created 3 interceptors (data/remote/interceptor/):
  * AuthInterceptor — adds "Authorization: Bearer <token>" header to every request except /auth/login and /auth/refresh.
  * TokenAuthenticator — OkHttp Authenticator that runs on 401. Uses refreshMutex to serialize concurrent refresh attempts. Double-check pattern: if another 401 already refreshed, just use the new token. On refresh failure → clearSession() + return null (propagates 401). Recursion guard via responseCount >= 2.
  * ErrorInterceptor — maps IOException → NoConnection/Timeout, HTTP 401 → Unauthorized, 403 → Forbidden, 404 → NotFound, 409 → Conflict, 422 → Validation, 429 → RateLimited, 5xx → ServerError. Parses the unified error body for code + message.
- Created data/remote/mapper/DtoMappers.kt — bidirectional mappers: UserDto→User, CustomerResponseDto→Customer, DeliveryResponseDto→Delivery. Plus parseIso8601ToMillis() and formatMillisToIso8601() helpers (uses java.time.Instant via core library desugaring).
- Created di/NetworkModule.kt — Hilt module providing:
  * SecureStorage (singleton, @ApplicationContext)
  * TokenManager (singleton)
  * NetworkJson (kotlinx.serialization Json with encodeDefaults=false for small payloads)
  * OkHttpClient (15s connect, 30s read/write, retryOnConnectionFailure, authenticator=TokenAuthenticator, application interceptors: AuthInterceptor + ErrorInterceptor, HttpLoggingInterceptor.HEADERS in debug only — no body logging to avoid token leaks)
  * Retrofit (baseUrl from BuildConfig.API_BASE_URL, kotlinx.serialization converter)
  * 4 API interfaces (AuthApi, CustomerApi, DeliveryApi, SyncApi)
- Created domain/model/User.kt — domain User (id, username, role, isActive, lastLoginAt).
- Created domain/repository/AuthRepository.kt — interface: login, logout, hasSession, verifySession, getCachedUser.
- Created data/repository/AuthRepositoryImpl.kt:
  * login → POST /auth/login → saveTokens → return User
  * logout → POST /auth/logout (best-effort) → clearSession
  * hasSession → TokenManager.hasSession()
  * verifySession → GET /auth/me → return User or null (on Unauthorized → null, on network error → cached user)
  * getCachedUser → reads from TokenManager
- Created 4 auth use cases: LoginUseCase, LogoutUseCase, HasSessionUseCase, VerifySessionUseCase.
- Updated di/RepositoryModule.kt — bind AuthRepository to AuthRepositoryImpl.
- Updated di/UseCaseModule.kt — provide 4 auth use cases.
- Rewrote presentation/auth/splash/SplashViewModel.kt — checks HasSessionUseCase → if false, navigate to Login. If true, calls VerifySessionUseCase (best-effort) → navigates to Home or Login.
- Rewrote presentation/auth/splash/SplashScreen.kt — observes SplashViewModel.state, navigates on NavigateToLogin/NavigateToHome.
- Rewrote presentation/auth/login/LoginViewModel.kt — LoginUiState (username, password, isLoading, isPasswordVisible, errorMessage, isSuccess). login() calls LoginUseCase, maps ApiException subtypes to Arabic messages. canSubmit computed property.
- Rewrote presentation/auth/login/LoginScreen.kt — bound to LoginViewModel. Username + password fields, visibility toggle, error message display, loading indicator, LaunchedEffect on isSuccess → onLoginSuccess callback.
- Wrote 4 test files (40 test methods):
  * DtoMappersTest.kt (10 tests) — UserDto→User (with null/malformed lastLoginAt), CustomerResponseDto→Customer (with null accuracy), DeliveryResponseDto→Delivery (DELIVERED + CANCELLED), parseIso8601ToMillis (malformed + valid), formatMillisToIso8601 round-trip.
  * ApiExceptionTest.kt (11 tests) — all 12 subclasses extend ApiException, Arabic messages for NoConnection/Timeout, code-carrying for Unauthorized/Forbidden/NotFound/Conflict, status for ServerError/HttpError, cause for ParseError, default message for Unknown.
  * TokenManagerTest.kt (9 tests) — initial state, hasSession true/false for blank, saveTokens persists all fields, updateAccessToken only writes access_token, clearSession removes all keys, getters read from storage, refreshMutex returns same instance.
  * LoginViewModelTest.kt (10 tests) — initial state, onUsernameChange/onPasswordChange, canSubmit, login success sets isSuccess, login unauthorized → Arabic message, login no-connection → Arabic message, login forbidden → Arabic message, resetSuccess, togglePasswordVisibility, login with blank username doesn't call repository.

Stage Summary:
- Phase 11 (Android ↔ Backend Integration) complete.
- 26 new Kotlin main files + 4 new test files added on top of Phase 10.
- Total Android: 101 Kotlin main files + 18 test files = 119 Kotlin files.
- Network layer architecture:
  * data/remote/dto/ — 6 DTO files matching the backend wire format exactly
  * data/remote/api/ — 4 Retrofit interfaces (14 endpoints total)
  * data/remote/interceptor/ — 3 interceptors (Auth, TokenAuthenticator, Error)
  * data/remote/mapper/ — DTO ↔ domain mappers with ISO 8601 timestamp parsing
  * core/network/ — ApiException sealed hierarchy with Arabic messages
  * core/security/ — SecureStorage (EncryptedSharedPreferences) + TokenManager
  * di/NetworkModule — Hilt wiring for the whole stack
- Auth flow complete:
  * SplashScreen → SplashViewModel checks HasSessionUseCase → if true, VerifySessionUseCase → navigate to Home or Login
  * LoginScreen → LoginViewModel → LoginUseCase → AuthRepository → AuthApi → POST /auth/login → saveTokens → navigate to Home
  * Auto-refresh: TokenAuthenticator handles 401 transparently with mutex for concurrency
  * Logout: AuthRepository.logout() → POST /auth/logout (best-effort) → clearSession
- Key security decisions:
  * Tokens stored in EncryptedSharedPreferences (AES-256-GCM, Keystore-backed)
  * HttpLoggingInterceptor.HEADERS in debug only — no body logging to avoid token leaks
  * TokenAuthenticator uses mutex to prevent concurrent refresh storms
  * Recursion guard (responseCount >= 2) prevents infinite refresh loops
  * ErrorInterceptor never exposes backend internals — Arabic user-facing messages
  * AuthRepository.logout() is best-effort: if the network call fails, local session is still cleared
- Next: Phase 12 (Offline Sync) — SyncWorker via WorkManager that reads pending sync_operations from Room, calls /sync, processes results (mark SYNCED / FAILED, handle CONFLICT by adopting server_state), retry with exponential backoff, network constraint (only run when CONNECTED).

---
Task ID: phase-12
Agent: main
Task: Phase 12 — Offline Sync: Build the SyncWorker (WorkManager CoroutineWorker) that drains the local sync queue, calls POST /sync, processes results (SUCCESS/CONFLICT/FAILED/IGNORED), applies server_changes (download direction), and persists the latest_sync_timestamp for incremental sync.

Work Log:
- Created data/sync/SyncPreferences.kt — DataStore-backed preferences storing: latest_sync_timestamp (the server watermark for incremental sync), last_sync_attempt_at, last_sync_success_at. Uses DataStore (not EncryptedSharedPreferences) because timestamps are not sensitive.
- Created data/sync/SyncPayloadBuilder.kt — builds SyncOperationDto from pending SyncOperationEntity + the related customer/delivery entity. Handles all 7 operation types (CREATE/UPDATE/DELETE_CUSTOMER, CREATE/UPDATE/COMPLETE/CANCEL_DELIVERY). For UPDATE_CUSTOMER includes updated_at for conflict detection. For COMPLETE/CANCEL_DELIVERY attaches the idempotency_key.
- Created data/sync/SyncResultProcessor.kt — processes the results array from /sync response:
  * SUCCESS → mark sync_operation SYNCED, apply server_state to entity (upsert with syncState=SYNCED)
  * CONFLICT → adopt server_state (latest-write-wins), mark operation SYNCED (resolved)
  * IGNORED → mark operation SYNCED (no longer applicable, e.g. DELETE on already-deleted entity)
  * FAILED → increment retry count, auto-FAILED at MAX_RETRIES=5
  * Unknown status → mark FAILED
  * Returns SyncStats (total, succeeded, conflicts, failed, ignored)
- Created data/sync/ServerChangeApplier.kt — applies server_changes (download direction) to Room:
  * CREATE/UPDATE_CUSTOMER → upsert CustomerEntity with syncState=SYNCED
  * DELETE_CUSTOMER → deleteById
  * CREATE/UPDATE/COMPLETE/CANCEL_DELIVERY → upsert DeliveryEntity with syncState=SYNCED
  * Idempotent — re-applying same change produces same state
  * Malformed changes are skipped without throwing (doesn't block others)
  * Enables multi-device sync: change on Device A → server → Device B receives as server_change
- Created data/sync/SyncWorker.kt — HiltWorker CoroutineWorker:
  * Reads pending sync_operations from Room
  * If empty → runDownloadOnlySync() to fetch server_changes
  * Builds SyncRequestDto with all pending operations + latest_sync_timestamp
  * Calls SyncApi.sync()
  * Processes results via SyncResultProcessor
  * Applies server_changes via ServerChangeApplier
  * Persists new latest_sync_timestamp + last_sync_success_at
  * Cleans up SYNCED rows older than 7 days
  * Returns Result.retry() on network/server errors, Result.success() otherwise
  * Even if some ops FAILED, returns success (permanent failures won't be fixed by retry)
- Created data/sync/SyncScheduler.kt — schedules SyncWorker via WorkManager:
  * scheduleImmediateSync() — OneTimeWorkRequest with ExistingWorkPolicy.KEEP (called after every local mutation)
  * schedulePeriodicSync() — PeriodicWorkRequest every 15 minutes (called on app startup)
  * Both use NetworkType.CONNECTED constraint + exponential backoff (30s → 1hr)
  * cancelAll() on logout
- Created core/network/NetworkMonitor.kt — monitors connectivity via ConnectivityManager:
  * isOnline: StateFlow<Boolean> for UI
  * observe(): Flow<NetworkState> for fine-grained monitoring
  * Checks NET_CAPABILITY_INTERNET + NET_CAPABILITY_VALIDATED (not just "connected")
  * Used by HomeViewModel to show Online/Offline indicator
- Created domain/usecase/sync/ScheduleSyncUseCase.kt — wraps SyncScheduler.scheduleImmediateSync()
- Created domain/usecase/sync/ObservePendingSyncCountUseCase.kt — wraps SyncRepository.observePendingCount()
- Created di/SyncModule.kt — provides SyncScheduler (singleton, @ApplicationContext)
- Updated di/UseCaseModule.kt — provides ScheduleSyncUseCase + ObservePendingSyncCountUseCase
- Updated data/repository/CustomerRepositoryImpl.kt — injects ScheduleSyncUseCase, calls scheduleSync() after every enqueueSyncOperation (add/update/delete customer)
- Updated data/repository/DeliveryRepositoryImpl.kt — same pattern, calls scheduleSync() after every delivery operation
- Updated WaselApp.kt — calls syncScheduler.schedulePeriodicSync() in onCreate()
- Updated presentation/home/HomeViewModel.kt:
  * Added ObservePendingSyncCountUseCase + NetworkMonitor injection
  * Combined 6 flows: customers, activeIds, driverLocation, selectedId, isOnline, pendingSyncCount
  * HomeUiState now includes isOnline + pendingSyncCount fields
- Created presentation/home/SyncStatusBadge.kt — compact pill showing:
  * 🟢 "متصل" (online, no pending) — green
  * 🟠 "غير متصل" (offline) — red, CloudOff icon
  * 🔵 "X بانتظار المزامنة" (online with pending) — primary, Sync icon
- Updated presentation/home/HomeScreen.kt — SyncStatusBadge below the search bar
- Added 6 new string resources (sync_pending_count, sync_online, sync_offline, sync_in_progress) in values/ + values-ar/
- Wrote 2 test files (18 test methods):
  * SyncResultProcessorTest.kt (9 tests) — SUCCESS marks SYNCED, SUCCESS with server_state upserts entity, CONFLICT adopts server_state + marks SYNCED, IGNORED marks SYNCED, FAILED increments retry, unknown status marks FAILED, empty results, mixed batch, delivery server_state upsert.
  * ServerChangeApplierTest.kt (9 tests) — CREATE_CUSTOMER inserts with SYNCED, UPDATE_CUSTOMER upserts, DELETE_CUSTOMER calls deleteById, CREATE_DELIVERY inserts, COMPLETE_DELIVERY updates with completed_at, multiple changes all applied, malformed change skipped, empty changes returns zero.

Stage Summary:
- Phase 12 (Offline Sync) complete.
- 11 new Kotlin main files + 2 new test files added on top of Phase 11.
- Total Android: 112 Kotlin main files + 20 test files = 132 Kotlin files.
- Sync architecture (Offline First fully realized):
  * Write path: UI → UseCase → Repository (writes to Room + enqueues SyncOperation + calls scheduleSync())
  * Sync path: ScheduleSyncUseCase → SyncScheduler → WorkManager → SyncWorker
  * Upload: SyncWorker reads pending ops → builds payload via SyncPayloadBuilder → POST /sync
  * Results: SyncResultProcessor marks ops SYNCED/FAILED, adopts server_state on CONFLICT
  * Download: ServerChangeApplier applies server_changes to Room (multi-device sync)
  * Watermark: SyncPreferences persists latest_sync_timestamp for incremental sync
- Key design decisions:
  * WorkManager with NetworkType.CONNECTED constraint — sync only runs when online
  * ExistingWorkPolicy.KEEP — 10 quick edits don't enqueue 10 syncs; the first one drains everything
  * Exponential backoff (30s → 1hr) — WorkManager handles retry timing
  * MAX_RETRIES=5 — after 5 failed attempts, operation is marked FAILED permanently (user can discard)
  * Download-only sync runs even when upload queue is empty — picks up multi-device changes
  * Conflict resolution on CONFLICT: adopt server_state (latest-write-wins) + mark operation SYNCED (resolved, don't retry)
  * IGNORED for DELETE on already-deleted entity — safe retry, no error shown to user
  * Periodic cleanup of SYNCED rows older than 7 days — keeps sync_operations table small
  * NetworkMonitor checks NET_CAPABILITY_VALIDATED — detects captive portals (connected but no internet)
- UI:
  * SyncStatusBadge in HomeScreen shows online/offline/pending count
  * Badge color-coded: green (online), red (offline), primary (pending)
  * Updates reactively via StateFlow
- The app is now FULLY offline-capable:
  * All writes go to Room first → UI updates immediately
  * SyncOperation enqueued for each mutation
  * scheduleSync() fires WorkManager immediately
  * WorkManager runs when network is available
  * Results processed → Room updated with server_state
  * Multi-device changes propagated via server_changes
- Next: Phase 13 (Delivery Flow) — wire StartDeliveryUseCase + CompleteDeliveryUseCase to the UI, Customer Details "Start Delivery" button, Active Delivery screen, arrival detection integration.

---
Task ID: phase-13
Agent: main
Task: Phase 13 — Delivery Flow: Wire the delivery state machine to the UI. StartDelivery from Customer Details, Active Delivery screen with state-driven buttons, complete/cancel flow, active delivery banner in Home.

Work Log:
- Created 5 delivery use cases:
  * StartDeliveryUseCase — calls DeliveryRepository.createDelivery(customerId), returns Delivery (ON_THE_WAY + startedAt=now)
  * TransitionDeliveryUseCase — calls repository.transition(deliveryId, newStatus), enforces state machine
  * CompleteDeliveryUseCase — wraps TransitionDeliveryUseCase with target=DELIVERED
  * CancelDeliveryUseCase — wraps TransitionDeliveryUseCase with target=CANCELLED
  * ObserveActiveDeliveryUseCase — combines ON_THE_WAY + ARRIVED flows, returns the active delivery (or null)
- Updated di/UseCaseModule.kt — provides all 5 new delivery use cases.
- Updated presentation/customers/CustomerDetailsViewModel.kt:
  * Injected StartDeliveryUseCase
  * Added isStartingDelivery, startedDeliveryId, startDeliveryError fields to UiState
  * startDelivery() calls the use case, on success sets startedDeliveryId (screen navigates to Active Delivery)
  * clearStartedDeliveryId() + clearStartDeliveryError() for cleanup
- Updated presentation/customers/CustomerDetailsScreen.kt:
  * Added onStartDelivery: (String) -> Unit parameter
  * "Start Delivery" button now calls viewModel::startDelivery
  * Button shows CircularProgressIndicator when isStartingDelivery
  * LaunchedEffect on startedDeliveryId → calls onStartDelivery(id) + clearStartedDeliveryId
  * SnackbarHost shows startDeliveryError
- Created presentation/delivery/active/ActiveDeliveryViewModel.kt:
  * HiltViewModel injecting DeliveryRepository, CustomerRepository, TransitionDeliveryUseCase, CompleteDeliveryUseCase, CancelDeliveryUseCase
  * _deliveryId flow + flatMapLatest on deliveryRepository.observeById
  * Combined with _aux state (isTransitioning, error)
  * ActiveDeliveryUiState with computed properties: canMarkArrived (ON_THE_WAY + !transitioning), canComplete (ARRIVED + !transitioning), canCancel (isActive + !transitioning), isFinished (terminal status)
  * markArrived() → transition to ARRIVED
  * complete() → completeDelivery use case (ARRIVED → DELIVERED)
  * cancel() → cancelDelivery use case (any non-terminal → CANCELLED)
  * customer cache loaded in parallel via customerRepository.getCustomer
  * isFinished becomes true when status is DELIVERED or CANCELLED → screen pops back
- Created presentation/delivery/active/ActiveDeliveryScreen.kt:
  * TopAppBar with back button
  * StatusBanner with color-coded status (orange=ON_THE_WAY, blue=ARRIVED, green=DELIVERED, red=CANCELLED)
  * Customer info (name + phone with call button)
  * Timestamp rows (started_at, arrived_at, completed_at)
  * State-driven buttons:
    - ON_THE_WAY: "وصلت إلى الزبون" (mark arrived) + Call + Cancel
    - ARRIVED: "تم التسليم" (complete) + Cancel
    - Terminal: no buttons (screen auto-pops)
  * Cancel shows confirmation dialog
  * LaunchedEffect on isFinished → onFinished() callback (pop back)
  * Loading state while delivery loads
  * Error snackbar
- Created presentation/home/ActiveDeliveryBanner.kt — orange banner shown on Home when there's an active delivery:
  * LocalShipping icon
  * "لديك توصيل نشط" + customer name (if available)
  * "متابعة التوصيل" label
  * Tappable → navigates to Active Delivery screen
- Updated presentation/home/HomeViewModel.kt:
  * Injected ObserveActiveDeliveryUseCase
  * Combined 7 flows: customers, activeIds, driverLocation, selectedId, isOnline, pendingSync, activeDelivery
  * HomeUiState now includes activeDeliveryId + activeDeliveryCustomerId
- Updated presentation/home/HomeScreen.kt:
  * Added onContinueDelivery: (String) -> Unit parameter
  * ActiveDeliveryBanner shown above the FABs when activeDeliveryId != null
  * Banner shows the active customer's name (looked up from state.customers)
- Updated presentation/navigation/Routes.kt — added activeDelivery(deliveryId) helper.
- Updated presentation/navigation/WaselNavHost.kt:
  * CustomerDetailsScreen now passes onStartDelivery callback → navigates to Routes.activeDelivery(deliveryId)
  * HomeScreen now passes onContinueDelivery callback → navigates to Routes.activeDelivery(deliveryId)
  * Added composable for Routes.ACTIVE_DELIVERY with deliveryId NavType.StringType argument
- Added 18 new string resources in values/ + values-ar/ for: active_delivery_title, delivery_status_* (4 statuses), delivery_mark_arrived, delivery_complete, delivery_cancel, delivery_cancel_confirm, delivery_started_at, delivery_arrived_at, delivery_completed_at, delivery_not_found, delivery_finished, active_delivery_banner, active_delivery_continue.
- Wrote 2 test files (20 test methods):
  * DeliveryUseCasesTest.kt (7 tests) — startDelivery calls createDelivery, startDelivery returns delivery, transitionDelivery delegates with target, completeDelivery → DELIVERED, cancelDelivery → CANCELLED, transitionDelivery propagates IllegalStateException, startDelivery propagates errors. Uses FakeDeliveryRepo with controllable return values + exception injection.
  * ActiveDeliveryUiStateTest.kt (13 tests) — canMarkArrived true/false (ON_THE_WAY, not ON_THE_WAY, transitioning), canComplete true/false (ARRIVED, ON_THE_WAY, transitioning), canCancel true (ON_THE_WAY + ARRIVED) / false (DELIVERED + CANCELLED + transitioning), isFinished true (DELIVERED, CANCELLED) / false (ON_THE_WAY, ARRIVED, null delivery), status returns delivery status or null.

Stage Summary:
- Phase 13 (Delivery Flow) complete.
- 10 new Kotlin main files + 2 new test files added on top of Phase 12.
- Total Android: 120 Kotlin main files + 22 test files = 142 Kotlin files.
- Delivery state machine fully wired:
  * Customer Details → "Start Delivery" button → StartDeliveryUseCase → Room (ON_THE_WAY) + sync queue → navigate to Active Delivery
  * Active Delivery screen shows status-driven buttons (mark arrived / complete / cancel)
  * State machine enforced both in domain (DeliveryStatus.canTransitionTo) and repository (DeliveryRepositoryImpl.transition)
  * Terminal states (DELIVERED, CANCELLED) auto-pop the screen via isFinished
  * Home screen shows Active Delivery banner when there's an active delivery
- Key UX decisions:
  * ON_THE_WAY shows "I arrived" (not "complete") — driver must confirm arrival before completing
  * ARRIVED shows "Complete delivery" — final step
  * Cancel available from any non-terminal state (with confirmation dialog)
  * Call button available during ON_THE_WAY (driver might need to call customer for directions)
  * Auto-pop on terminal status — no extra "done" button needed
  * Banner on Home screen — driver can resume an active delivery from anywhere
- Navigation graph now has 10 routes: splash, login, home, customers, history, settings, add_customer, customer_details, edit_customer, capture_location, active_delivery.
- Next: Phase 14 (Routing & ETA) — Mapbox Directions API integration: RoutingEngine interface, MapboxRoutingEngine implementation, CalculateRouteUseCase, display distance + ETA on Active Delivery screen, fallback to straight-line distance on routing failure.

---
Task ID: phase-14
Agent: main
Task: Phase 14 — Routing & ETA: Integrate Mapbox Directions API with RoutingEngine abstraction, CalculateRouteUseCase with straight-line fallback, display distance + ETA on Active Delivery screen.

Work Log:
- Created domain/model/RouteResult.kt:
  * RouteResult data class: distanceMeters, durationSeconds, geometry (List<LatLng>), instructions, isFallback.
  * Computed properties: distanceKm, durationMinutes (min 1), formattedDistance ("3.7 km" or "850 m"), formattedDuration ("9 min" or "1 hr 30 min").
  * RouteInstruction data class: instruction, distanceMeters, durationSeconds, maneuverType, modifier, position.
- Created core/maps/RoutingException.kt — sealed class: NoRouteFound, RoutingNetworkError, RoutingError.
- Created core/maps/RoutingEngine.kt — interface: `suspend fun calculateRoute(from: LatLng, to: LatLng): RouteResult`. Infrastructure concern (Mapbox/OSRM), not domain.
- Created core/maps/mapbox/MapboxRoutingEngine.kt:
  * Uses MapboxDirections builder with PROFILE_DRIVING_TRAFFIC, steps=true, GEOMETRY_POLYLINE6, voiceInstructions, bannerInstructions.
  * awaitResponse() wraps the async call in suspendCoroutine.
  * Decodes polyline6 geometry into List<LatLng>.
  * Extracts turn-by-turn instructions from route legs + steps.
  * Maps IOException → RoutingNetworkError, empty routes → NoRouteFound, other → RoutingError.
- Updated core/maps/MapsModule.kt — provides MapboxRoutingEngine as singleton RoutingEngine.
- Created domain/usecase/routing/CalculateRouteUseCase.kt:
  * Calls routingEngine.calculateRoute(from, to).
  * On ANY failure (NoRouteFound, RoutingNetworkError, RoutingError, generic Exception) → straightLineFallback().
  * Fallback: distance = Haversine(from, to), duration = distance / 8.33 m/s (30 km/h), geometry = [from, to], isFallback = true.
  * Always returns a result — UI never shows "no route", always has at least approximate distance + ETA.
- Updated di/UseCaseModule.kt — provides CalculateRouteUseCase.
- Updated presentation/delivery/active/ActiveDeliveryViewModel.kt:
  * Injected CalculateRouteUseCase + LocationProvider.
  * Added route, isCalculatingRoute, routeError fields to UiState.
  * calculateRoute() method: gets current driver location → calls CalculateRouteUseCase → stores RouteResult.
  * Auto-calculation triggered by LaunchedEffect on delivery+customer+route state.
  * AuxState extended with route/isCalculatingRoute/routeError.
- Created presentation/delivery/active/RouteInfoCard.kt — card showing:
  * Calculating state: spinner + "جاري حساب الطريق…"
  * Ready (real route): "3.7 km" + "9 min" (no prefix)
  * Ready (fallback): "≈ 3.7 km" + "≈ 9 min" + hint "مسار تقريبي"
  * Error state: "تعذر حساب الطريق"
  * RouteMetric composable with icon + label + value.
- Updated presentation/delivery/active/ActiveDeliveryScreen.kt:
  * LaunchedEffect auto-calculates route when ON_THE_WAY + customer loaded + no route yet.
  * RouteInfoCard shown between StatusBanner and customer info (only when ON_THE_WAY).
- Added 6 new string resources (route_calculating, route_distance, route_eta, route_fallback_hint, route_recalculate, route_error) in values/ + values-ar/.
- Wrote 2 test files (18 test methods):
  * CalculateRouteUseCaseTest.kt (8 tests) — returns real route on success, falls back on NoRouteFound/RoutingNetworkError/RoutingError/generic Exception, fallback duration = distance/speed, fallback geometry = [from, to], fallback for same point = 0 distance. Uses FakeRoutingEngine with controllable return + exception injection.
  * RouteResultTest.kt (10 tests) — distanceKm conversion, durationMinutes min 1 + rounding, formattedDistance (km vs m), formattedDuration (min vs hr+min vs hr only), isFallback default false + set true.

Stage Summary:
- Phase 14 (Routing & ETA) complete.
- 8 new Kotlin main files + 2 new test files added on top of Phase 13.
- Total Android: 126 Kotlin main files + 24 test files = 150 Kotlin files.
- Routing architecture:
  * core/maps/RoutingEngine — interface (SDK-agnostic)
  * core/maps/RoutingException — sealed exception hierarchy
  * core/maps/mapbox/MapboxRoutingEngine — production impl using Mapbox Directions API
  * domain/usecase/routing/CalculateRouteUseCase — with straight-line fallback
  * presentation/delivery/active/RouteInfoCard — UI card showing distance + ETA
- Key design decisions:
  * Fallback strategy: CalculateRouteUseCase ALWAYS returns a result. If the routing engine fails (offline, API error, no route), it falls back to Haversine straight-line distance with estimated ETA (30 km/h). The UI shows "≈" prefix for fallback values + a hint "مسار تقريبي".
  * Polyline6 decoding: implemented from scratch (no external dependency) — standard polyline algorithm with 1e6 precision factor.
  * Auto-calculation: ActiveDeliveryScreen auto-calculates the route when the delivery is ON_THE_WAY + customer is loaded + no route has been calculated yet. The driver sees distance + ETA immediately on entering the screen.
  * Route only shown when ON_THE_WAY — once the driver marks "arrived", the route is no longer relevant.
  * Mapbox profile: driving-traffic (traffic-aware routing) for accurate ETA in urban Iraq.
  * Steps + voice + banner instructions enabled — needed for Phase 15 (Navigation).
- Next: Phase 15 (Navigation & Alerts) — Mapbox Navigation SDK integration: NavigationEngine interface, MapboxNavigationEngine implementation, turn-by-turn navigation, voice alerts, re-routing on deviation.
