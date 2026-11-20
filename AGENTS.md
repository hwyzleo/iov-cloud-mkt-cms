# SPEC-Driven Development 约束文档
- 参考 Notion Page ID: 37d0ec7a4b9280bd8c84c922f57f6d3e

# 全局研发环境与行为准则
- 参考 Notion Page ID: 37d0ec7a4b928005aee8d863af5b2518

# 开源车联网研发与行为准则
- 参考 Notion Page ID: 37d0ec7a4b92802d8915c7c310e24486

# 当前项目需求文档
- 参考 Notion Page ID: 41e0ec7a4b92803da315d6aff7fb8568
- 变更记录 Notion Page ID: 41e0ec7a4b9280fa8caddb7fd6111f6e

# 当前项目设计文档
- 参考 Notion Page ID: 41e0ec7a4b9280acb6dff9e3ffa73678
- 变更记录 Notion Page ID: 41e0ec7a4b9280e983a4dd1460d24cc7

# 当前项目系统架构文档
- 参考 Notion Page ID: 41e0ec7a4b928021810bfd01fe3be046

# 当前项目功能架构文档
- 参考 Notion Page ID: 41e0ec7a4b928065ab16eaff4d790219

# 当前项目数据库文档
- 参考 Notion Page ID: 41e0ec7a4b92807c9f07f081b4964823

# 当前项目错误码文档
- 参考 Notion Page ID: 41e0ec7a4b928055a419d3302f9bfee4

# graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
