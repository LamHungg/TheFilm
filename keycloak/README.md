# Keycloak cho THEFILM (moi truong phat trien)

Dat cac file `docker-compose.yml`, `run.sh`, `stop.sh` va thu muc `keycloak/` o thu muc goc cua repo (cung cap voi `BE`, `FE`, `database`).

## Chay va dung

    ./run.sh          # bat Oracle (neu co container oracle26ai), Keycloak, roi chay BE
    ./stop.sh         # dung BE va Keycloak
    ./stop.sh --all   # dung them ca container Oracle

Lan dau chay can docker va mang de keo image Keycloak. Cho Keycloak san sang mat khoang 30 den 60 giay.

## Thong tin

| Muc | Gia tri |
|---|---|
| Trang quan tri Keycloak | http://localhost:8081 (admin / admin) |
| Realm | thefilm |
| Issuer (BE cau hinh san trong BE/.env) | http://localhost:8081/realms/thefilm |
| Client cho FE | thefilm-web (public, PKCE S256, redirect http://localhost:4200/* va 4300/*) |
| Client cho BE | thefilm-api (bearer-only) |
| Mat khau moi tai khoan mau | Thefilm@123 |

## Tai khoan mau

Ten dang nhap la email. `sub` cua token trung voi `oidc_subject` trong bang `app_user` cua migration V005, nen BE tu lien ket dung nhan vien hoac khach hang.

| Vai tro | Tai khoan |
|---|---|
| CHAIN_ADMIN | khanh.nguyen@thefilm.example |
| HR | huong.tran@thefilm.example |
| CINEMA_MANAGER (HCM-Q1) | tuan.vo@thefilm.example |
| CASHIER (HCM-Q1) | lan.nguyen@thefilm.example |
| CUSTOMER | thanhmai.nguyen@example.com |

Danh sach day du nam trong `keycloak/thefilm-realm.json` (23 nhan vien co tai khoan va 14 khach hang).

## Lay token de thu API (Postman hoac curl)

    curl -s -X POST http://localhost:8081/realms/thefilm/protocol/openid-connect/token \
      -d grant_type=password -d client_id=thefilm-web \
      -d username=khanh.nguyen@thefilm.example -d password='Thefilm@123' | jq -r .access_token

Dung gia tri nhan duoc lam `Authorization: Bearer <token>`. Khi chay cach nay khong dung profile `devauth`.

## Luu y

- Realm duoc nap tu file JSON luc container khoi dong. Muon nap lai sau khi sua file: `./stop.sh` roi `docker compose down -v` roi `./run.sh`.
- Chi dung cho dev: mat khau, admin/admin va `sslRequired: none` khong duoc dung o moi truong that.
- `V005` phai da duoc nap vao Oracle thi tai khoan moi khop duoc voi `app_user`.
