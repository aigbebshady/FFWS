# Handoff: J6 Engagement Action Tracker (MSS / Maven build)

| | |
|---|---|
| **Status** | Design agreed. Ready to build. |
| **Build target** | Maven Smart System (MSS). Use the platform's own building tools only: no outside app, database, login, or email service. |
| **Working name** | USINDOPACOM J6 Engagement Action Tracker |
| **Prepared** | 19 Nov 2026 |

---

## 0. Instructions for the builder (read first)

You are picking up a design that is already settled. Your job is to build it inside the MSS environment.

1. **Don't redesign the workflow.** Build what this document describes. If the platform can't do something, propose the closest fit and flag it to the owner.
2. **Run the feasibility check first** (Section 9). Report results before you build reminders.
3. **Build Phase 1 first** (Section 10). Create the *full* data model on day one, including objects Phase 2 needs, so nothing needs a migration later.
4. **Use fictional test data only** until the owner says otherwise. Follow the environment's classification and marking rules for everything you create.
5. **Confirm tool names.** This document uses Foundry-style terms (Ontology object types, Action types, Workshop, Functions, Automate). MSS may name or limit these differently. Use the MSS equivalent available to the builder account.
6. **Use the defaults in Section 12** until the owner confirms each open decision. Keep every default in one settings place, never hard-coded across the app.

---

## 1. The problem

After J6 engagement events, action items go unworked. Nobody knows their status. Leadership can't get a straight answer without a round of emails.

The tracker fixes three things:

- **Ownership:** every action has one named person who owns it.
- **Silence:** the tool flags late work *and* owners who stopped checking in, on its own.
- **Reporting:** anyone can produce a paste-ready brief for the boss in one click.

**Design rule: easy beats complete.** A routine owner update must take under a minute. Every extra required field lowers how many people update.

---

## 2. Users, roles, permissions

| Role | Who |
|---|---|
| **Member** | Anyone with app access |
| **Owner** | The one person assigned to an action |
| **Tracker Manager** | A small named group (1-3 people) |
| **Leadership** | Boss / division chief. Reads the Boss Update tab or the pasted brief. Same rights as a Member. |

| Capability | Member | Owner (own actions) | Tracker Manager |
|---|---|---|---|
| View and search all actions | Yes | Yes | Yes |
| Quick Add actions | Yes (default, confirm) | Yes | Yes |
| Post the official update (incl. No Change, Close) | No | Yes | Yes, on the owner's behalf (labeled) |
| Add a comment | Yes | Yes | Yes |
| Edit or delete **own** comment | Yes | Yes | Yes |
| Remove someone else's comment | No | No | Yes (logged) |
| Reassign owner | No | No | Yes (logged, reason) |
| Change due date | No | No | Yes (logged, reason required) |
| Reopen a closed action | No | No | Yes (logged) |
| Copy Brief | Yes | Yes | Yes |

**Rules**

- One owner per action. A person, never an office.
- Only the owner, or a manager acting for them, posts the official status. **Comments never change status, check-in date, or progress date.** A teammate's comment must not make a stale action look current.
- Every manager change writes to the item history: who, when, old value, new value, reason.
- Hide buttons a user can't use. Enforce the same rules server-side (submission criteria or equivalent), not only in the UI.

---

## 3. Data model

Names are suggestions. Keep the meaning.

### 3.1 Engagement

| Property | Type | Required | Notes |
|---|---|---|---|
| engagement_id | string, primary key | Yes | e.g., `ENG-0007` |
| name | string | Yes | e.g., "Bilateral C4 Talks 26-2" |
| countries | string array | Yes | Supports multinational events. Searchable. |
| event_start_date | date | Yes | |
| event_end_date | date | No | |
| lead | user | No | |

Link: one Engagement has many Action Items.

### 3.2 Action Item

| Property | Type | Required | Notes |
|---|---|---|---|
| action_id | string, primary key | Yes | Human-readable, sequential, e.g., `A-0142` |
| title | string | Yes | Short sentence that starts with a verb |
| details | string | No | |
| engagement_id | link to Engagement | Yes | |
| owner | user | Yes | One person |
| status | enum | Yes | `Not Started`, `On Track`, `At Risk`, `Blocked`, `Complete` |
| due_date | date | Yes | Current due date |
| original_due_date | date | Yes | Set at creation. Never changes. |
| slip_count | integer | Yes | +1 each time a manager moves the due date later |
| created_at / created_by | timestamp / user | Yes | |
| last_checkin_at | timestamp | Yes | Set to created_at on creation. Updated by **every** owner update, including No Change. |
| last_progress_at | timestamp | Yes | Set to created_at on creation. Updated by every owner update **except** No Change. |
| latest_update_text | string | No | Text of the most recent progress update |
| help_needed_text | string | When Blocked | "What help or decision do you need?" |
| needs_leadership_decision | boolean | When Blocked | `true` puts the action in "Decisions needed" |
| closed_at / closed_by | timestamp / user | When Complete | |
| closure_note | string | When Complete | What got done |
| closure_reason | enum | When Complete | `Done`, `Overtaken by events` (confirm list) |
| classification | per environment | Per marking rules | Feeds portion marks in Copy Brief (Section 8) |
| **Stored flags** | | | Refreshed per Section 4.4 |
| owes_update | boolean | | |
| update_overdue | boolean | | |
| action_overdue | boolean | | |
| decision_needed | boolean | | |
| stalled | boolean | | |
| next_checkin_due | date | | |
| severity_score | integer | | For sorting |

### 3.3 Owner Update (the official status history; one row per update)

| Property | Type | Notes |
|---|---|---|
| update_id | primary key | |
| action_id | link to Action Item | |
| posted_by | user | |
| on_behalf_of | user, nullable | Set when a manager posts for the owner |
| posted_at | timestamp | |
| update_type | enum | `Progress`, `No Change`, `Close`, `Reopen` |
| status_after | enum | |
| text | string | |
| help_needed_text | string | |
| needs_leadership_decision | boolean | |

### 3.4 Comment

| Property | Type | Notes |
|---|---|---|
| comment_id | primary key | |
| action_id | link to Action Item | |
| author | user | |
| created_at | timestamp | |
| text | string | |
| edited_at | timestamp, nullable | Show "(edited)" when set |
| is_removed | boolean | **Soft delete.** Hidden from the thread, kept for the record. |
| removed_by / removed_at | user / timestamp | |

### 3.5 History Event (audit trail)

`event_id`, `action_id`, `actor`, `at`, `event_type` (`Created`, `Reassigned`, `Due Date Changed`, `Comment Removed`, `Reopened`), `old_value`, `new_value`, `reason`.

### 3.6 Brief Log

`brief_id`, `generated_at`, `generated_by`, `since_date`, plus a counts snapshot (open, decisions needed, action overdue, missing updates, closed). Used to default "since last brief" and, later, to show trends.

### 3.7 Tracker Settings (one record or config)

| Setting | Default |
|---|---|
| cycle_length_days | 14 |
| checkin_due_weekday | Wednesday |
| cycle_anchor_date | A past deadline that fell on that weekday, e.g., `2026-01-07` |
| stall_threshold_days | 28 |
| reminder_lead_days | 2 (Monday reminder for a Wednesday deadline) |
| reminder_time | 0800 local |
| time_zone | HQ local time (Hawaii, HST). Confirm. |
| manager_group | Named list of Tracker Managers |

---

## 4. Check-in cycle, status rules, flags

### 4.1 Fixed check-in cycle (shared by everyone)

Everyone's check-ins come due on the same day. That gives one predictable deadline, one reminder per owner, and a fresh brief.

```
next_checkin_due     = first deadline on or after today
                       (deadlines fall every cycle_length_days, counted from cycle_anchor_date)
current_cycle_start  = next_checkin_due - (cycle_length_days - 1) days
previous_cycle_start = current_cycle_start - cycle_length_days
previous_checkin_due = current_cycle_start - 1 day
```

Do all date math in one time zone (`time_zone` setting).

### 4.2 What each event changes

| Event | last_checkin_at | last_progress_at | status | latest_update_text |
|---|---|---|---|---|
| Action created | now | now | Not Started | empty |
| Owner posts **No Change** | now | unchanged | unchanged | unchanged |
| Owner posts On Track / At Risk / Blocked | now | now | new value | new text |
| Owner posts **Complete** | now | now | Complete | closure note |
| Anyone comments | unchanged | unchanged | unchanged | unchanged |
| Manager reassigns or moves the due date | unchanged | unchanged | unchanged | unchanged |

**"No Change" confirms the owner checked in.** Always show the check-in date *separately* from the last progress date. Leadership can then ask two questions: when did the owner last check in, and when did they last report progress?

### 4.3 Flags (open = status is not Complete)

```
owes_update     = open AND last_checkin_at < current_cycle_start
update_overdue  = open AND last_checkin_at < previous_cycle_start   -- skipped the last deadline
action_overdue  = open AND due_date < today
decision_needed = open AND status = Blocked AND needs_leadership_decision
stalled         = open AND NOT update_overdue AND status <> Blocked
                  AND (today - last_progress_at) >= stall_threshold_days
```

| Flag | Label shown | Color |
|---|---|---|
| decision_needed | Decision needed | Red |
| action_overdue | Action overdue · N days | Red |
| update_overdue | Update overdue | Amber |
| owes_update (not overdue) | Update due Wed 25 Nov | Neutral |
| stalled | No progress N days | Purple / neutral |
| slip_count > 0 | Slipped N× | Neutral |

Always pair color with a text label. Never use color alone.

**Severity sort** (most urgent first): decision_needed = 5, action_overdue = 4, update_overdue = 3, Blocked = 2, stalled = 1. Add them up. Ties sort by due date, earliest first.

### 4.4 Where to compute flags

Flags depend on today's date, so stored values go stale overnight.

- Write one function, e.g. `computeFlags(action, settings, now)`, as the single source of truth.
- Store its results on the Action Item. Refresh them (a) inside every write action and (b) with a daily scheduled job shortly after midnight local time.
- Reminders and filters use the stored flags. If the UI can compute live (function-backed columns), it may, but it must use the same function.

---

## 5. Screens (one app, one home screen)

One app works for everyone. Owners see what they owe. Whoever prepares the brief pulls it from the same screen. Three tabs sit at the top: **My Actions · All Open · Boss Update**. A **+ Quick Add** button is always visible.

### 5.1 The action row (same in every list)

- Title, action ID, engagement name, countries
- Status and owner
- Due date, plus "was <original date>" when slipped
- Flag chips (Section 4.3)
- Latest owner update (two lines max), then `Progress <date> · Check-in <date>`
- Button: **Post Update** for the owner, **Post for owner** for managers, hidden for everyone else
- Clicking the row opens the action detail panel

### 5.2 Tab 1: My Actions (default landing tab)

- Header: "Updates I owe (N). Check-ins due Wed 25 Nov. 'No change' counts."
- **Updates I Owe:** my open actions where `owes_update`. Highlighted. Sorted by severity.
- **My other open actions**
- **Managers only: Missing owner updates** across all actions (`update_overdue`), with Post for owner and Reassign.
- Empty state: "You're current for this cycle."

### 5.3 Tab 2: All Open

- One search box. It matches action ID, title, engagement name, countries, owner name, owner office.
- Filter chips: All · Decision needed · Action overdue · Update overdue · Blocked · No progress 28+ days. Optional filters: engagement, owner.
- Sorted by severity, then due date.

### 5.4 Tab 3: Boss Update

- "Changes since" date. Defaults to the last Brief Log date, or 7 days ago if no brief exists yet.
- Five count tiles: Open · Decisions needed · Action overdue · Missing owner updates · Closed since <date>.
- Five sections, in brief order:
  1. Decisions needed
  2. Blocked or overdue (leave out items already listed under Decisions needed)
  3. Missing owner updates (owner skipped the last deadline)
  4. Checked in, no progress in 28+ days
  5. Closed since <date>
- Each item shows the owner and the latest update with its date.
- **Copy Brief** button (Section 8).

### 5.5 Action detail panel

- Facts: owner and office, engagement, countries, due date (original date and slip count), last progress, last check-in
- Latest owner update. If Blocked, also show "Need: <help_needed_text>".
- **Post Update** button (owner or manager). Other users see: "Only the owner or a tracker manager posts the official update. You can comment below."
- Manager tools: Reassign · Change due date · Reopen
- Official updates timeline, newest first. Include No Change entries. Label posts made on the owner's behalf: "Posted by X for Y".
- Comments thread: add, edit own, delete own. Managers can remove anyone's. Hint under the box: "Comments don't count as the owner's check-in."
- Item history (manager changes, comment removals)

### 5.6 Quick Add (after an event)

1. Pick an existing engagement (searchable) or create one (name, countries, event date).
2. Enter rows: **Action**, **Owner** (person picker), **Due date**. All required.
3. "Add another action" adds a row with the engagement already set, so users enter it once.
4. Save creates every row at once. Confirmation: "N actions added to <engagement>."
5. Remember the last-used engagement for the session.
6. Error messages name the row and the field, e.g., "Row 3: pick an owner."

### 5.7 Post Update form

Opens from the row button, the detail panel, or a reminder link.

| Choice | Text rules | Effect |
|---|---|---|
| **No Change** | Note optional ("Anything to add?") | Check-in only |
| **On Track** / **At Risk** | "What changed since your last update?" required | Progress update |
| **Blocked** | "What changed?" optional. "What help or decision do you need?" **required**. Checkbox: "This needs a leadership decision." | Progress update. Checkbox puts it first in the brief. |
| **Complete** | "What got done?" required. Closure reason: Done / Overtaken by events. | Closes the action |

- Preselect the current status. No Change is one click away.
- When a manager posts for the owner, show: "You're posting for <owner>. The record will show your name."
- After saving: recompute flags, show "Update posted", drop the item from that owner's reminder for this cycle.

---

## 6. Write operations (Action types or equivalent)

Every change goes through one of these. No direct edits to objects.

| Operation | Who | Inputs | Validation | Effects |
|---|---|---|---|---|
| Quick Add | Member | Engagement (or new engagement fields); rows of title, owner, due date | All fields required. Warn (don't block) if a due date is in the past. | Create Action Items: status Not Started, check-in and progress = now, original_due_date = due_date. History "Created". Compute flags. |
| Post Update | Owner; Manager on behalf | Action, choice, text, help-needed text, decision checkbox, closure reason | Rules in 5.7 | Create Owner Update. Set fields per 4.2. Recompute flags. |
| Add Comment | Member | Action, text | Not empty | Create Comment |
| Edit Comment | Author only | Comment, text | Not empty | Set text and edited_at |
| Delete Own Comment | Author only | Comment | | is_removed = true. History "Comment removed by author". |
| Remove Comment | Manager | Comment, optional reason | | is_removed = true. History with reason. |
| Reassign | Manager | Action, new owner, reason | New owner differs from current | Set owner. History with old owner, new owner, reason. |
| Change Due Date | Manager | Action, new date, reason | Reason required. Date must differ. | Set due_date. slip_count +1 if the new date is later. History. original_due_date never changes. |
| Reopen | Manager | Action, reason | Status is Complete | Status = On Track. Clear closed fields. Owner Update type Reopen. History. |
| Log Brief | Anyone | Since date | | Create Brief Log (runs on Copy Brief) |

---

## 7. Reminders

**Goal:** one reminder per owner per cycle, covering every action they owe, each with a direct link.

- **When:** `reminder_lead_days` before the deadline, at `reminder_time` (default: Monday 0800 for a Wednesday deadline).
- **Who:** owners with at least one open action where `owes_update` is true.
- **Content:**
  - Subject: `Tracker: 3 updates due Wed 25 Nov`
  - Body: one line per action (ID, title, due date, flags) with a **Post update** link
  - Footer: "'No change' counts as a check-in."
- **Link target, best first:**
  1. Opens the Post Update form for that action
  2. Opens that action's detail panel or object view
  3. Opens the My Actions tab, where Updates I Owe sits at the top
- **Clearing:** an action drops off once the owner checks in that cycle. An owner who is fully current gets no reminder.
- **No other notifications in v1.** Comments don't notify. Missed deadlines aren't chased with more messages. They show up under "Missing owner updates" in the boss brief, and that visibility does the chasing.
- **Build hint (Foundry-style):** an Automate rule on a schedule, over the object set where `owes_update` = true, with a notification effect whose recipients come from the `owner` property. Check whether it can group per recipient. If it can't, build one message per owner with a function-backed notification. Fall back to one notification per action only if neither works.

---

## 8. Copy Brief

Plain text that pastes cleanly into email, slides, or chat. No tables. Same order as the Boss Update tab.

```
<OVERALL CLASSIFICATION BANNER>
J6 ENGAGEMENT ACTIONS - STATUS AS OF 19 NOV 2026
Open: 14 | Decisions needed: 2 | Action overdue: 3 | Missing owner updates: 2 | Closed since 12 NOV: 2

1. DECISIONS NEEDED (2)
(<portion mark>) A-0103 Decide whether to fund a second test window in FY27 [Bilateral C4 Talks 26-2 / Japan]
    Owner: Maj Kim | Due 28 Nov
    Need: Leadership decision. Fund from the engagement budget, or defer to FY28.
    Latest (18 Nov): Cost estimate complete for both options.

2. BLOCKED OR OVERDUE (3)
(<portion mark>) A-0101 Send updated US POC roster to partner J6 staff [Bilateral C4 Talks 26-2 / Japan]
    Owner: TSgt Patel | Due 14 Nov - 5 days late, slipped 1x
    Latest (20 Oct): Roster drafted. Waiting on two branch chiefs to confirm names.

3. MISSING OWNER UPDATES (2)
(<portion mark>) A-0101 Send updated US POC roster to partner J6 staff - TSgt Patel - last check-in 2 Nov

4. CHECKED IN, NO PROGRESS IN 28+ DAYS (1)
(<portion mark>) A-0106 Confirm translation support for the next staff talks - TSgt Patel - last progress 15 Oct

5. CLOSED SINCE 12 NOV (2)
(<portion mark>) A-0115 Send thank-you letters to the partner delegation - closed 16 Nov by SMSgt Alvarez: Letters signed and delivered.

<OVERALL CLASSIFICATION BANNER>
```

(Example values are fictional.)

- Empty sections print "None."
- An action may appear in section 2 and section 3. That's intended: section 3 is the accountability list.
- **Markings:** the banner is the highest classification among the included items. Each item line starts with its portion mark. Use the platform's own markings if it applies them. Confirm the format with the security manager. Don't guess.
- When someone copies the brief, write a Brief Log row. "Since" then defaults to that date next time.
- If the platform has no copy-to-clipboard control, show the brief in a read-only text box the user can select and copy.

---

## 9. Feasibility check (do this first, report back)

1. **Builder access:** can the builder account create object types, write operations, an app (Workshop or equivalent), functions, and scheduled automations? Who approves publishing to users?
2. **Notifications:** which channels exist on this network (in-platform, email)? Can a rule run on a schedule? Can recipients come from the `owner` property? Can messages group per recipient? **Can a link open a specific action, ideally with the Post Update form open?**
3. **Identity:** person picker for owner; current user available for permission checks; a manager group.
4. **Markings:** how classification applies to objects and properties; how the Copy Brief text should be marked.
5. **Copy:** does the app builder offer a copy-to-clipboard control?
6. **Time zone:** confirm which time zone drives "today" and deadlines.

---

## 10. Build phases

**Phase 1 (MVP, owner's priority)**
- Full data model (Section 3), including Phase 2 objects
- Quick Add
- Post Update with all five choices, one click from every list row
- Flags, severity sort, daily refresh
- Three tabs with search
- Copy Brief and Brief Log

**Phase 2**
- Comments: add, edit own, delete own, manager remove
- Manager tools: reassign, change due date with slips, reopen, post on behalf
- Reminders (per the feasibility result)
- Item history view

**Phase 3 (later)**
- Trend line in the brief ("Open: 18 to 14 since last brief")
- Closure stats per engagement
- Archive of old closed actions

Comments were part of the original requirement. Move them into Phase 1 if leadership wants them at launch.

---

## 11. Acceptance tests

| # | Setup | Do | Expect |
|---|---|---|---|
| 1 | Action created today | Nothing | No flags. Not in Updates I Owe this cycle. |
| 2 | Last check-in inside the previous cycle | Nothing | "Update due <date>" chip. In Updates I Owe. Not Update Overdue. |
| 3 | Last check-in before the previous cycle started | Nothing | Update Overdue. Listed under Missing owner updates. |
| 4 | Action from test 3 | Owner posts No Change | Update Overdue clears. Check-in = now. Progress date and status unchanged. |
| 5 | Open action due yesterday | Nothing | "Action overdue · 1 day" |
| 6 | Any open action | Non-owner comments | Check-in date, progress date, flags unchanged |
| 7 | Owner picks Blocked | Saves without help text | Validation error. Nothing saved. |
| 8 | Owner posts Blocked with the decision box checked | | decision_needed. Listed first in Boss Update and the brief. |
| 9 | Owner picks Complete | Saves without a note | Validation error |
| 10 | Owner posts Complete with a note | | Leaves open lists. Appears under Closed since. |
| 11 | Manager moves the due date later with a reason | | slip_count +1. original_due_date unchanged. History entry with the reason. |
| 12 | Manager changes the due date without a reason | | Validation error |
| 13 | Member who isn't the owner opens an action | | No Post Update button. Comment box works. |
| 14 | Manager posts for the owner | | Update shows "Posted by <manager> for <owner>" |
| 15 | Owner posts No Change for 28+ days with no progress update | | "No progress N days" chip. Listed in brief section 4. |
| 16 | Author deletes own comment | | Hidden from the thread. History "Comment removed by author". |
| 17 | Copy Brief | | Text follows Section 8 order. Brief Log row created. Next Boss Update defaults "since" to that date. |
| 18 | Owner owes 3 actions | Reminder runs | One reminder listing all 3, each link opens the right action |
| 19 | Search "Japan" in All Open | | Returns actions from engagements whose countries include Japan |

---

## 12. Open decisions (defaults in use until the owner confirms)

| Decision | Default |
|---|---|
| Check-in cycle | Every 14 days |
| Deadline day | Wednesday. Best set to the day before the boss's weekly sync. |
| Reminder timing | 2 days before the deadline, 0800 local |
| Stalled threshold | 28 days without progress |
| Time zone | HQ local time (Hawaii, HST) |
| Tracker Managers | To be named (1-3 people) |
| Who can Quick Add | Any member |
| Can owners close their own actions | Yes. Managers can reopen. |
| Closure reasons | Done, Overtaken by events |
| Comment delete | Soft delete, logged in history |
| Brief day and recipient | To be named |
| Classification handling | Confirm with the security manager |

---

## 13. Design choices to keep (and why)

- **Two overdue flags.** Late work and a silent owner are different problems with different fixes.
- **No Change counts as a check-in, shown apart from progress.** An owner can't look current just by clicking a button.
- **Stalled flag.** Catches the owner who clicks No Change every cycle while nothing moves.
- **Slips stay visible.** Moving a due date clears Action Overdue, so the slip count keeps the record honest.
- **Decision checkbox on Blocked.** Keeps partner delays out of the boss's decision list. Only real calls reach it.
- **Comments never touch the clocks.** A teammate's note can't make a stale action look current.
- **One reminder per owner per cycle.** Fewer messages means people actually read them.

---

## 14. References (commercial Foundry docs; confirm MSS equivalents)

- Automate notification effect: https://palantir.com/docs/foundry/automate/effect-notification/
- Action type notifications: https://palantir.com/docs/foundry/action-types/notifications/
- Set up an action notification: https://www.palantir.com/docs/foundry/action-types/set-up-notification
- Example, weekly report: https://palantir.com/docs/foundry/automate/example-weekly-report/
- Example, notify owners from an object property: https://palantir.com/docs/foundry/automate/example-dynamic-contract-owner/
