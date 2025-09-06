# Code Review Checklist

### 1. Naming and Style

- [ ] Naming conventions are respected:
  - PascalCase for class names
  - camelCase for methods and variables
  - UPPER_SNAKE_CASE for constants
- [ ] Names are meaningful and not too short

---

### 2. Class Design & Encapsulation

- [ ] Classes have a single clear responsibility (SRP)
- [ ] Fields have the minimum required visibility and scope
- [ ] Objects are properly initialized via constructors, no custom init methods

---

### 3. Methods and Logic

- [ ] Methods have a single clear and coherent purpose
- [ ] Common logic is extracted into reusable methods, no code duplication

---

### 4. Object-Oriented Usage

- [ ] Static members are used only for constants or stateless utility methods
- [ ] Inheritance is applied only to model semantic specialization ("is-a" relationships)

---

### 5. Collections and Streams

- [ ] Enhanced for loops are used unless indexed access is explicitly required
- [ ] Data structures are chosen appropriately for their purpose (e.g., List vs Map)
- [ ] Stream pipelines are well-structured and side-effect free
- [ ] forEach is used only for terminal actions, not to replace iteration

---

### 6. Persistence (JPA/Hibernate)

- [ ] Relationships are used only when semantically and technically necessary
- [ ] No direct SQL queries, use JPQL instead

---

### 7. Error Handling and Resource Management

- [ ] Resources are always closed properly (try-with-resources or finally block)
- [ ] Exceptions are handled explicitly and meaningfully, no silent or overly generic catch blocks

---

### 8. Readability and Clean Code

- [ ] Constants are used instead of hardcoded values
- [ ] No commented-out code or debug statements left in the code
