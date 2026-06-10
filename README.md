# GlobalNews AI

> 멀티리전 DR(Disaster Recovery) 기반 AI 뉴스 인텔리전스 플랫폼

해외 뉴스를 AWS Bedrock(Claude) 기반 AI가 한국어로 번역·요약·해석해 제공하고, 사용자가 기사에 대해 챗봇과 대화할 수 있는 서비스입니다. 더불어 서울 리전 장애 시 도쿄 리전으로 자동 전환되는 멀티리전 DR 아키텍처를 설계·구현하고, 그 동작을 **수치로 검증**한 프로젝트입니다.

2022년 카카오 판교 데이터센터 화재 사례를 설계의 출발점으로 삼아, 당시 드러난 *수동 전환 의존 · 물리적 미분리 · DR 사이클 미검증* 세 가지 문제를 각각 **CloudFront Origin Failover 자동 전환 / 멀티리전 분리 / DR 시나리오 반복 검증**으로 해결하는 구조를 목표로 했습니다.

<!-- TODO: 배포 도메인 / 데모 링크 / 시연 영상 링크가 있으면 추가 (예: https://globalnews.life) -->

---
## 메인페이지 
<img width="1051" height="652" alt="image" src="https://github.com/user-attachments/assets/965b4790-4921-4cb1-96f2-7b78f8afd3d8" />

## 챗봇 사용화면 
<img width="1117" height="500" alt="image" src="https://github.com/user-attachments/assets/3fd7400b-f9b9-443a-81ff-05283e41c9b0" />


---

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [DR 시나리오 및 검증 결과](#dr-시나리오-및-검증-결과)
- [CI/CD 파이프라인](#cicd-파이프라인)
- [모니터링](#모니터링)
- [핵심 구현 포인트](#핵심-구현-포인트)
- [프로젝트 구조](#프로젝트-구조)
- [팀 구성](#팀-구성)

---

## 주요 기능

- **해외 뉴스 자동 수집 및 AI 번역·요약** — GNews API로 해외 뉴스를 주기적으로 수집하고, AWS Bedrock(Claude)이 핵심 내용을 한국어로 정리합니다.
- **AI 챗봇 해석 서비스** — 기사에 대한 질문을 입력하면 AI가 맥락과 배경 지식까지 포함해 답변합니다. 이전 대화 내역과 뉴스 컨텍스트를 시스템 프롬프트에 결합해 뉴스 맞춤형 응답을 생성합니다.
- **후기 및 커뮤니티 기능** — 사용자가 기사에 대한 별점·코멘트 기반 후기를 남길 수 있습니다.
- **멀티리전 고가용성** — 서울 리전 장애 시 도쿄 리전으로 자동 우회하여 서비스 연속성을 보장합니다.

---

## 기술 스택


<img width="1263" height="522" alt="image" src="https://github.com/user-attachments/assets/ca1abe6f-8dd4-4c07-baa6-c89344128e21" />


---

## 시스템 아키텍처

<img width="1202" height="689" alt="image" src="https://github.com/user-attachments/assets/cd4629fd-7ed6-4e88-9be6-4138f19c01f4" />

---

<img width="1060" height="653" alt="image" src="https://github.com/user-attachments/assets/a1b6ab03-6ce2-4fad-8686-35703ed271e6" />

---

<img width="1117" height="654" alt="image" src="https://github.com/user-attachments/assets/488f6f0b-43c0-435e-886f-8338e53d0010" />

---

## DR 시나리오 및 검증 결과

실제 장애를 주입하여 DR 체계의 동작을 정량적으로 검증했습니다. 사전에 목표를 선언하고 실측으로 달성 여부를 증명하는 방식을 사용했습니다.

### 1단계 — 데이터 복제 검증 (RPO)
Aurora Global DB의 리전 간 복제 지연을 Locust 부하 테스트로 측정.

| 부하 조건 | 평균 복제 지연 | 최대 복제 지연 |
|-----------|---------------|----------------|
| 부하 없음 (기준선) | 73ms | 1,081ms |
| Locust 200 VU | 67ms | 1,040ms |
| API 경유 최대 부하 (WAL 4.1MB/s) | 28ms | 944ms |

→ 모든 조건에서 평균 복제 지연 100ms 미만, 최대 1초 내외로 **RPO 1초 미만**을 보장. 데이터 유실률 0%에 근접.

### 2단계 — 파드/노드 오토스케일링 검증
부하 발생 시 HPA가 파드를, Karpenter가 Spot 노드를 자동 확장하고, 부하 종료 후 자동 축소되는 것을 2회에 걸쳐 검증 (1·2차 동일 패턴으로 재현성 입증).

| 항목 | 결과 |
|------|------|
| 파드 자동 확장 (HPA) | 1 → 14개 |
| 노드 자동 확장 (Karpenter Spot) | 1 → 5개 |
| 부하 종료 후 Scale-In | 노드 1개 / 파드 3개로 축소 |

### 3단계 — CloudFront Origin Failover
서울 ALB에 503 장애를 주입(ALB Listener Rule `fixed-response 503`)했을 때 CloudFront가 5xx를 감지해 실시간으로 도쿄 Origin으로 우회하는지 검증.

| 항목 | 결과 |
|------|------|
| 서울 → 도쿄 전환 | 실시간 (DNS 전파 대기 불필요) |
| Failover 중 사용자 응답 | HTTP 200 유지 |
| 원복 후 응답 | HTTP 200 복귀 확인 |

→ 사용자가 장애를 인지하지 못하는 수준의 자동 전환을 비파괴 방식(인프라 무손상·즉시 원복)으로 검증.

**목표 요약**

| 지표 | 목표 | 결과 |
|------|------|------|
| RPO | < 1초 | 실측 평균 28~73ms, 최대 1,081ms |
| Failover RTO | < 20분 (Pilot Light) | Karpenter 노드 프로비저닝 + Pod 기동 포함 |
| Failback | 운영자 수동 절차 (DR Runbook) | — |

---

## CI/CD 파이프라인

GitHub Actions(CI) + Argo CD(GitOps CD)의 **책임 분리** 구조.

- **CI (GitHub Actions)** — 빌드/테스트/이미지 푸시. AWS 리전 장애 범위 밖에 CI 서버를 배치하여, 서울 리전이 완전히 다운된 상황에서도 긴급 배포가 가능하도록 설계.
- **CD (Argo CD, GitOps)** — Source 저장소와 Manifest 저장소를 분리하고, 멀티리전 EKS에 일관된 배포를 자동화.
- **이미지 태깅** — dev는 `dev-<sha>`, prod는 `prod-<날짜>-<sha>` 형식으로 ECR에서 환경·날짜·커밋을 한눈에 식별.
- **프론트엔드 캐싱** — Vite 해시 적용 JS/CSS는 1년 캐싱, 진입점 `index.html`은 캐시 금지로 설정해 배포 즉시 새 버전 로드.

<!-- TODO: 보고서의 CI/CD 아키텍처 다이어그램을 docs/ 에 추가 후 경로 연결 -->

---

## 모니터링

- **Prometheus / Grafana** — 클러스터·서비스 메트릭 수집 및 대시보드
- **CloudWatch** — Aurora Global DB 복제 지연 메트릭, Failover 감지 알람
- **Slack** — 장애·Failover 알림 연동

Grafana는 IAM 권한 기반으로 운영하며, 장애 감지부터 복구 과정 전체를 실시간 가시화합니다.

---
## 클러스터 요약 
<img width="1081" height="405" alt="image" src="https://github.com/user-attachments/assets/07ddd8a6-237a-41cd-8c8e-6662c676ef34" />

---
## 앱 노드 요약 
<img width="1078" height="321" alt="image" src="https://github.com/user-attachments/assets/c6593e90-4087-44f8-8f74-179323a70586" />

---


## 프로젝트 구조
```
.
├── src/main/java/...      # Spring Boot 백엔드
├── frontend/             # React + Vite 프론트엔드
├── infra/                # Terraform 모듈 (storage, network, ingress, eks, db)
├── .github/workflows/    # GitHub Actions CI
└── manifests/            # Argo CD용 K8s 매니페스트 (별도 레포일 수 있음)
-->
```

---

## 팀 구성
<img width="1246" height="670" alt="image" src="https://github.com/user-attachments/assets/072c7a15-2314-4a2a-8d18-787f96ef5135" />



