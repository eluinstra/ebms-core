# EbMS API Package

This package contains the API layer for the EbMS messaging system.

## Package Structure

The API package is organized by **feature** (use case-driven):

### EbMS Feature (`ebms/`)

- **Model** (`model/`) - Message-related DTOs and models
- **Exception** (`exception/`) - EbMS-specific exceptions
- **DAO** (`dao/`) - Data access interfaces (API-boundary)
- **SOAP** (`soap/`) - SOAP/WS-* implementations
- **REST** (`rest/`) - REST API implementations

### CPA Feature (`cpa/`)

- **Model** (`model/`) - External types (CollaborationProtocolAgreement)
- **Exception** (`exception/`) - CPA-specific exceptions
- **Repository** (`repository/`) - Repository interfaces (API contract)
- **Validator** (`validator/`) - Validation logic
- **SOAP** (`soap/`) - SOAP/WS-* implementations
- **REST** (`rest/`) - REST API implementations

### URL Mapping Feature (`url/`)

- **Exception** (`exception/`) - URL-specific exceptions
- **Repository** (`repository/`) - Repository interfaces
- **SOAP** (`soap/`) - SOAP/WS-* implementations
- **REST** (`rest/`) - REST API implementations

### Certificate Mapping Feature (`certificate/`)

- **Model** (`model/`) - Certificate DTOs
- **Exception** (`exception/`) - Certificate-specific exceptions
- **Repository** (`repository/`) - Repository interfaces
- **SOAP** (`soap/`) - SOAP/WS-* implementations
- **REST** (`rest/`) - REST API implementations

## Architecture Patterns

### Two-Layer Repository Pattern

Each feature has **TWO repository interfaces**:

|   Feature   |                   API Repository                   |                   Common Repository                   |             Purpose              |
|-------------|----------------------------------------------------|-------------------------------------------------------|----------------------------------|
| CPA         | `api.cpa.CPARepository`                            | `common.cpa.CPARepository`                            | Full CRUD vs Minimal             |
| URL         | `api.cpa.url.URLMappingRepository`                 | `common.cpa.url.URLMappingRepository`                 | Full CRUD vs Simple              |
| Certificate | `api.cpa.certificate.CertificateMappingRepository` | `common.cpa.certificate.CertificateMappingRepository` | Full CRUD vs Simple              |
| EbMS        | `api.ebms.EbMSDAO`                                 | `common.dao.EbMSDAO`                                  | API-boundary vs Core persistence |

### Repository Interface Architecture

- **API Repository**: Defines the contract for the REST/SOAP layer
  - Full CRUD operations
  - Used by controllers to handle REST/SOAP requests
  - Package: `api.*.repository` or `api.*.dao`
- **Common Repository**: Used by domain services
  - Minimal operations (exists/get)
  - Used by `CPAManager`, `CPAUtils`, etc.
  - Package: `common.*.repository`

Both interfaces **coexist** and serve different purposes.

## Keep in API Package

### Repository Interfaces

- `api.cpa.CPARepository` - Full CRUD operations
- `api.cpa.url.URLMappingRepository` - Full CRUD operations
- `api.cpa.certificate.CertificateMappingRepository` - Full CRUD operations
- `api.ebms.EbMSDAO` - API-specific DAO methods

### Validator Classes

- `api.cpa.CPAValidator` - Validates CPA XML against schema
- `api.ebms.MessagePropertiesValidator` - Validates message properties

### External Types

- `CollaborationProtocolAgreement` - External schema, DO NOT MOVE

## See Also

- `common/` - Domain layer (CPAManager, domain services)
- `common/dao/` - Common DAO implementations
- `common/cpa/` - CPA domain services

