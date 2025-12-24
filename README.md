# SmartTeamMate – Intelligent Team Formation System

SmartTeamMate is a Java-based intelligent team formation system that automatically generates **balanced teams** using participant data. The system applies **rule based evaluation, personality aware grouping, and skill level optimization** to support data driven team construction.

This project emphasizes **object oriented design, system modeling, and automated testing**, making it well aligned with AI and Data Science fundamentals.

---

## Key Features

- **Survey Handling**
  - Allow players to complete the survey
  - Saves the player data into a csv
    
- **Automated Team Formation**
  - Generates teams based on configurable team size
  - Ensures balanced distribution of skills and roles

- **Personality-Aware Logic**
  - Considers personality types such as *Leader*, *Thinker*, and *Balanced*
  - Enforces minimum and maximum personality constraints per team

- **Skill Balancing Engine**
  - Iteratively minimizes skill variance across teams
  - Uses controlled player swaps while preserving constraints

- **Rule Based Validation**
  - Centralized team rules and validation logic
  - Detects invalid teams and attempts automatic repair

- **CSV Based Data Handling**
  - Player data is uploaded and validated from CSV files
  - Formed teams can be exported back to CSV

- **Comprehensive Testing**
  - Unit tests for team-building logic
  - Concurrency tests for parallel execution
  - File integrity checks for CSV operations

---

## System Architecture

The system follows object oriented architecture:

### main
- `AppController`
- `Main`

### model 
- `Player`
- `Team`
- `Game`
- `Role`

### rules
- `TeamRules`

### repository
- `CSVhandler`
- `PlayerRepository`
- `TeamRepository`

### service 
- `ClassificationResult`
- `ConsoleSurveyHandler`
- `DatasetCheker`
- `PersonalityClassifier`
- `SkillBalancer`
- `SurveyHandler`
- `SurveyValidator`
- `TeamBalancer`
- `TeamBuilder`
- `TeamEvaluator`
- `TeamIssues`
- `TeamService`

System behavior and structure are documented using:
- UML Use Case Diagrams  
- Activity Diagrams  
- Sequence Diagrams  
- Class Diagrams (Entity focused)

---

## Main Use Cases

1. **Complete the Survey**
   - Players submit role, skill level, and personality data
   - Inputs are validated and appended to a CSV file

2. **Upload CSV File**
   - Organizer uploads participant data
   - File is validated before processing

3. **Initiate Team Formation**
   - Organizer defines team size
   - System builds, validates, balances, and displays teams

---

## Technologies Used

- **Language:** Java  
- **Concepts:**
  - Object Oriented Programming
  - Rule Based Systems
  - Constraint Validation
  - Iterative Optimization
- **Testing:** JUnit 5   
- **Data Handling:** CSV files  

---
